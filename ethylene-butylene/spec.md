# Abstract 
Butylene is a human-friendly data serialization language based on Unicode.

### Design Goals
Butylene is designed with several paradigms in mind:
1. Backwards compatible with [JSON](https://www.rfc-editor.org/rfc/rfc8259).
2. Human-readable.
3. Terse.
4. Simple.
5. Unambiguous.

# Introduction
Butylene is a serialization language inspired by HJSON, JSON, and YAML. It is designed to be a superset of JSON — all
valid JSON is valid Butylene (but not all valid Butylene is valid JSON).