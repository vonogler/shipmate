import {
  Button,
  Checkbox,
  Col,
  Divider,
  Form,
  Input,
  InputNumber,
  Row,
  Space,
  Typography
} from 'antd';
import { DeleteOutlined } from '@ant-design/icons';

type DraftReviewItemField = {
  key: number;
  name: number;
};

type DraftReviewItemFieldsProps = {
  field: DraftReviewItemField;
  index: number;
  onRemove: (name: number) => void;
};

export default function DraftReviewItemFields({ field, index, onRemove }: DraftReviewItemFieldsProps) {
  return (
    <div className="review-item-row" key={field.key}>
      <div className="review-item-title">
        <Typography.Text strong>품목 {index + 1}</Typography.Text>
        <Button
          danger
          icon={<DeleteOutlined />}
          onClick={() => onRemove(field.name)}
          aria-label={`품목 ${index + 1} 삭제`}
        />
      </div>
      <Form.Item name={[field.name, 'orderDraftItemCandidateId']} hidden>
        <Input />
      </Form.Item>
      <Row gutter={8}>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'itemName']} label="품목명" rules={[{ required: true, message: '품목명을 입력하세요.' }]}>
            <Input placeholder="종이박스" />
          </Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'itemSpec']} label="규격">
            <Input placeholder="300x200x150" />
          </Form.Item>
        </Col>
        <Col xs={12} md={6}>
          <Form.Item name={[field.name, 'itemQuantity']} label="수량" rules={[{ required: true, message: '수량을 입력하세요.' }]}>
            <Input placeholder="200" />
          </Form.Item>
        </Col>
        <Col xs={12} md={6}>
          <Form.Item name={[field.name, 'itemUnit']} label="단위" rules={[{ required: true, message: '단위를 입력하세요.' }]}>
            <Input placeholder="장" />
          </Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'matchedUnitPrice']} label="단가 후보">
            <InputNumber min={0} precision={0} className="full-width" placeholder="450" />
          </Form.Item>
        </Col>
        <Col xs={24}>
          <Form.Item name={[field.name, 'lineMemo']} label="품목 메모">
            <Input placeholder="로고 인쇄 없음" />
          </Form.Item>
        </Col>
      </Row>
      <Divider className="inner-divider" />
      <Typography.Title level={4} className="mini-title">포장재 주문제작</Typography.Title>
      <Row gutter={8}>
        <Col xs={8}>
          <Form.Item name={[field.name, 'packagingWidth']} label="가로"><Input placeholder="300" /></Form.Item>
        </Col>
        <Col xs={8}>
          <Form.Item name={[field.name, 'packagingDepth']} label="세로"><Input placeholder="200" /></Form.Item>
        </Col>
        <Col xs={8}>
          <Form.Item name={[field.name, 'packagingHeight']} label="높이"><Input placeholder="150" /></Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'materialName']} label="재질"><Input placeholder="골판지 B골" /></Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'productionQuantity']} label="제작 수량"><Input placeholder="200" /></Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'requestedDueDate']} label="납기 희망일"><Input placeholder="2026-06-30" /></Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Space wrap className="checkbox-row">
            <Form.Item name={[field.name, 'printRequired']} valuePropName="checked"><Checkbox>인쇄 필요</Checkbox></Form.Item>
            <Form.Item name={[field.name, 'designFileProvided']} valuePropName="checked"><Checkbox>디자인 파일 있음</Checkbox></Form.Item>
            <Form.Item name={[field.name, 'sampleRequested']} valuePropName="checked"><Checkbox>샘플 요청</Checkbox></Form.Item>
          </Space>
        </Col>
      </Row>
      <Divider className="inner-divider" />
      <Typography.Title level={4} className="mini-title">판촉/맞춤 제작</Typography.Title>
      <Row gutter={8}>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'customProductType']} label="제품 종류"><Input placeholder="머그컵, 쇼핑백, 스티커" /></Form.Item>
        </Col>
        <Col xs={24} md={12}>
          <Form.Item name={[field.name, 'imprintMethod']} label="각인/인쇄 방식"><Input placeholder="실크 인쇄, 레이저 각인" /></Form.Item>
        </Col>
        <Col xs={24} md={8}>
          <Form.Item name={[field.name, 'itemColor']} label="색상"><Input placeholder="화이트" /></Form.Item>
        </Col>
        <Col xs={24} md={16}>
          <Form.Item name={[field.name, 'customOptionText']} label="옵션"><Input placeholder="손잡이 추가, 무광 코팅" /></Form.Item>
        </Col>
        <Col xs={24}>
          <Space wrap className="checkbox-row">
            <Form.Item name={[field.name, 'proofConfirmed']} valuePropName="checked"><Checkbox>시안 확인</Checkbox></Form.Item>
            <Form.Item name={[field.name, 'productionConfirmed']} valuePropName="checked"><Checkbox>제작 확정</Checkbox></Form.Item>
          </Space>
        </Col>
      </Row>
    </div>
  );
}
