# ShipMate Docs

This repository stores documentation for ShipMate AI.

## Documentation Policy

- YAML files are the source of truth for structured project information.
- OpenAPI YAML is the source of truth for API documentation.
- HTML files are generated or maintained for browser-friendly viewing.
- HTML links must use relative paths so they work from local files and GitHub Pages.
- Do not edit generated HTML as the primary source when a matching YAML file exists.

## Domain Folder Rules

Android documentation belongs under:

```text
shipmate-doc/android/
  yaml/
  html/
```

Admin documentation for `shipmate-admin` belongs under:

```text
shipmate-doc/admin/
  yaml/
  html/
```

Shared documentation belongs under:

```text
shipmate-doc/common/
  yaml/
  html/
shipmate-doc/api/
shipmate-doc/policies/
```

Shared machine-readable assets belong under:

```text
shipmate-common/data/yaml/
  field-names.yaml
  master-codes.yaml
  error-codes.yaml
```

Do not place Android-only documents in `shipmate-doc/admin/`.
Do not place Admin-only documents in `shipmate-doc/android/`.
Do not place common design documents in the root `yaml/` folder.
Do not place field names, master codes, or error code number files in `shipmate-doc/data/`.

## HTML Entry Point

Open this file in a browser:

```text
html/index.html
```
