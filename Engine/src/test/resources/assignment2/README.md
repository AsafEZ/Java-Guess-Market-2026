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
