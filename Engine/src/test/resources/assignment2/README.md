# Assignment 2 XML fixtures

These files are original Assignment 2 schema and XML test materials supplied for the exercise. The XML fixtures are intentionally stored only under test resources. Do not edit them to make tests pass, including fixtures that contain deliberate business-rule errors.

## Source checksums

| File | Classification | SHA-256 |
| --- | --- | --- |
| `GM-EX2-Schema.xsd` | Official Assignment 2 schema; production copy is in `Engine/src/main/resources` | `59AD9A2ADE5C6F5030522BEF19F60AE92763F4C74CCF335D0611BBFE0A91822E` |
| `xml/valid/small.xml` | Valid Assignment 2 XML | `84AEFE7762C16944586137FBB9D32C370AF50F6B052DADAAE0D8D1C8B98E8D44` |
| `xml/valid/multiple.xml` | Valid Assignment 2 XML | `B6D2215C684A51B9C5D5C7C87E7DD70F1A82B3CD9DD9F2267116A3E43484F9F5` |
| `xml/invalid-business-rules/error-2.xml` | Schema-valid but business-invalid because `initial-cash=0` | `73E51948533ABCFB66A1901BFB5A9A65C056891A0D84BD1E9E37F5E07409FD8C` |
| `xml/invalid-business-rules/error-3.xml` | Schema-valid but business-invalid because a Market Maker references a missing event | `3F57666436007183A5715E4F8973A782BF587915A4260EDB9FC277CE76D3A437` |

The checksums identify the original supplied files. The repository copies were verified byte-for-byte when imported. Specific `ErrorCode` values for business-validation failures are implementation decisions; they are not defined by the supplied files themselves.

## Validation matrix

| Fixture | XSD result | Purpose or single schema rule | Business validation in 11B.5 |
| --- | --- | --- | --- |
| `xml/valid/small.xml` | Valid | Instructor-supplied small v2 document | Yes, as a complete v2 input |
| `xml/valid/multiple.xml` | Valid | Instructor-supplied multi-event v2 document | Yes, as a complete v2 input |
| `xml/invalid-business-rules/error-2.xml` | Valid | Proves that `initial-cash=0` is permitted by the XSD | Yes, reject non-positive initial cash |
| `xml/invalid-business-rules/error-3.xml` | Valid | Proves that the XSD does not enforce Market Maker event references | Yes, reject the missing event reference |
| `xml/edge-cases/schema-boundaries.xml` | Valid | Proves reversed root order, one option, `b=0`, `initial=-1`, `d=0`, `initial-cash=0`, and zero/negative event ids are permitted by the XSD | Yes, apply the relevant business rules after schema validation |
| `xml/invalid-schema/missing-users.xml` | Invalid | Required `GM-users` is absent | No; rejected by schema validation |
| `xml/invalid-schema/missing-events.xml` | Invalid | Required `GM-events` is absent | No; rejected by schema validation |
| `xml/invalid-schema/misspelled-commission.xml` | Invalid | Uses v1 `comision` instead of v2 `commission` | No; rejected by schema validation |
| `xml/invalid-schema/multiple-trading-methods.xml` | Invalid | One `GM-method` violates `xs:choice` by containing LMSR and Order Book | No; rejected by schema validation |
| `xml/invalid-schema/missing-order-book-attribute.xml` | Invalid | Required Order Book attribute `d` is absent | No; rejected by schema validation |
| `xml/invalid-schema/invalid-allow-mint.xml` | Invalid | `allow-mint` is neither exactly `true` nor `false` | No; rejected by schema validation |
| `xml/invalid-schema/too-many-options.xml` | Invalid | Contains three options while the XSD permits at most two | No; rejected by schema validation |
| `xml/invalid-schema/invalid-event-sequence.xml` | Invalid | `description` appears before the required leading `id` | No; rejected by schema validation |
| `xml/invalid-schema/non-integer-event-id.xml` | Invalid | Event id is not an `xs:int` | No; rejected by schema validation |

The files under `xml/invalid-schema` are derived, minimal, well-formed fixtures. Each one intentionally violates only the schema rule named in the table. The `schema-boundaries.xml` fixture groups related values that are schema-valid but may be rejected later by Java business validation.
