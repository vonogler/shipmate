package com.shipmate.api.customer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Repository
public class CustomerRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CustomerRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Customer save(Customer customer) {
        jdbcTemplate.update("""
                        insert into tb_customers (
                            customer_id, merchant_id, customer_name, customer_phone_number,
                            default_recipient_name, default_recipient_phone_number, default_postal_code,
                            default_address, default_address_detail, favorite_item_names,
                            deleted_at, created_at, updated_at
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on duplicate key update
                            merchant_id = values(merchant_id),
                            customer_name = values(customer_name),
                            customer_phone_number = values(customer_phone_number),
                            default_recipient_name = values(default_recipient_name),
                            default_recipient_phone_number = values(default_recipient_phone_number),
                            default_postal_code = values(default_postal_code),
                            default_address = values(default_address),
                            default_address_detail = values(default_address_detail),
                            favorite_item_names = values(favorite_item_names),
                            deleted_at = values(deleted_at),
                            updated_at = values(updated_at)
                        """,
                customer.customerId(),
                customer.merchantId(),
                customer.customerName(),
                customer.customerPhone(),
                customer.defaultRecipientName(),
                customer.defaultRecipientPhone(),
                customer.defaultPostalCode(),
                customer.defaultAddress(),
                customer.defaultAddressDetail(),
                writeJson(customer.favoriteItemNames()),
                toTimestamp(customer.deletedAt()),
                toTimestamp(customer.createdAt()),
                toTimestamp(customer.updatedAt())
        );
        jdbcTemplate.update("delete from tb_customer_price_items where customer_id = ?", customer.customerId());
        for (int index = 0; index < customer.priceItems().size(); index++) {
            CustomerPriceItem item = customer.priceItems().get(index);
            jdbcTemplate.update("""
                            insert into tb_customer_price_items (
                                customer_price_item_id, customer_id, item_name_keyword, item_spec_keyword,
                                item_unit, unit_price, sort_order, created_at, updated_at
                            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                            """,
                    item.customerPriceItemId(),
                    customer.customerId(),
                    item.itemNameKeyword(),
                    item.itemSpecKeyword(),
                    item.itemUnit(),
                    item.unitPrice(),
                    index,
                    toTimestamp(customer.updatedAt()),
                    toTimestamp(customer.updatedAt())
            );
        }
        return customer;
    }

    public Optional<Customer> findById(String customerId) {
        return jdbcTemplate.query(
                "select * from tb_customers where customer_id = ?",
                customerRowMapper(),
                customerId
        ).stream().findFirst();
    }

    public List<Customer> findRecent(int limit) {
        return jdbcTemplate.query(
                "select * from tb_customers order by updated_at desc limit ?",
                customerRowMapper(),
                limit
        );
    }

    public List<Customer> findAll() {
        return jdbcTemplate.query(
                "select * from tb_customers order by updated_at desc",
                customerRowMapper()
        );
    }

    private RowMapper<Customer> customerRowMapper() {
        return (rs, rowNum) -> {
            String customerId = rs.getString("customer_id");
            return new Customer(
                    customerId,
                    rs.getString("merchant_id"),
                    rs.getString("customer_name"),
                    rs.getString("customer_phone_number"),
                    rs.getString("default_recipient_name"),
                    rs.getString("default_recipient_phone_number"),
                    rs.getString("default_postal_code"),
                    rs.getString("default_address"),
                    rs.getString("default_address_detail"),
                    readFavoriteItems(rs.getString("favorite_item_names")),
                    findPriceItems(customerId),
                    toOffsetDateTime(rs.getTimestamp("deleted_at")),
                    toOffsetDateTime(rs.getTimestamp("created_at")),
                    toOffsetDateTime(rs.getTimestamp("updated_at"))
            );
        };
    }

    private List<CustomerPriceItem> findPriceItems(String customerId) {
        return jdbcTemplate.query(
                """
                        select * from tb_customer_price_items
                        where customer_id = ?
                        order by sort_order asc, id asc
                        """,
                (rs, rowNum) -> new CustomerPriceItem(
                        rs.getString("customer_price_item_id"),
                        rs.getString("item_name_keyword"),
                        rs.getString("item_spec_keyword"),
                        rs.getString("item_unit"),
                        rs.getBigDecimal("unit_price")
                ),
                customerId
        );
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (Exception exception) {
            throw new IllegalStateException("고객 즐겨찾는 품목을 JSON으로 변환하지 못했습니다.", exception);
        }
    }

    private List<String> readFavoriteItems(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.asList(objectMapper.readValue(json, String[].class));
        } catch (Exception exception) {
            throw new IllegalStateException("고객 즐겨찾는 품목을 JSON에서 읽지 못했습니다.", exception);
        }
    }

    private static Timestamp toTimestamp(OffsetDateTime value) {
        return value == null ? null : Timestamp.from(value.toInstant());
    }

    private static OffsetDateTime toOffsetDateTime(Timestamp value) {
        return value == null ? null : value.toInstant().atOffset(ZoneOffset.UTC);
    }
}
