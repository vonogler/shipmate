# ShipMate Common

This repository stores shared machine-readable assets used across ShipMate projects.

## Ownership

Field names, master codes, status codes, and error message code numbers belong here, not in `shipmate-doc`.

```text
shipmate-common/
  data/
    yaml/
      field-names.yaml
      master-codes.yaml
      error-codes.yaml
```

## File Roles

- `data/yaml/field-names.yaml`: canonical field names shared by Android, Admin, API, and future services.
- `data/yaml/master-codes.yaml`: controlled business code lists. This file is the future source for RDB master code tables and seed data.
- `data/yaml/error-codes.yaml`: error code numbers, messages, and client display guidance.

Documentation about these files can live in `shipmate-doc/common/`, but the source files live in `shipmate-common/`.
