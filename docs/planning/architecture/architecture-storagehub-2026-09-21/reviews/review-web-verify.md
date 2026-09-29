# Review — Lens web-verify · Architecture Spine StorageHub

- **Ngày review:** 2026-09-22
- **Đối tượng:** `docs/planning/architecture/architecture-storagehub-2026-09-21/ARCHITECTURE-SPINE.md` (updated 2026-09-22, mục Stack verify web 2026-09-21)
- **Lens:** web-verify — mọi decision đã commit phải được web-research/reality-check, không assert từ training data; greenfield thì kiểm tra live defaults của starter.
- **Verdict:** **PASS** — toàn bộ version đã verify trong spine vẫn đúng tính đến 2026-09-22; có 1 mục spine để "verify khi init" nay đã verify được (springdoc 3.1.1), 2 nhãn version cần chỉnh nhỏ (JUnit, TypeScript), và 1 nhóm "starter defaults đã đổi so với quan niệm phổ biến" cần nhóm sinh viên nắm.

## Phương pháp

WebSearch bị rate-limit (hạn mức reset 2026-10-13) nên review dùng **truy vấn trực tiếp nguồn chính thức ngày 2026-09-22**: Maven Central (`repo1.maven.org`), npm registry (`registry.npmjs.org`), start.spring.io (metadata + sinh project thật), endoflife.date, GitHub wiki spring-projects, springdoc.org, docs.spring.io. Đây là các nguồn gốc (primary) — mạnh hơn kết quả search.

## 1. Spring Boot — CONFIRMED

| Hạng mục | Kết quả |
| --- | --- |
| GA mới nhất | **4.1.1** — đúng như spine. `4.2.0-M1` mới chỉ là milestone, không phải stable. Maven metadata `lastUpdated 2026-08-20` khớp ngày spine ghi. |
| Patch mới hơn 4.1.1 | **Không có** trên Maven Central. Initializr có sẵn snapshot `4.1.2.BUILD-SNAPSHOT` — tức 4.1.2 sắp ra nhưng chưa GA. |
| Support window | 4.1: OSS đến **31-07-2027** (commercial 31-07-2028); 3.5 OSS **đã kết thúc 30-06-2026** (patch cuối 3.5.16). Khớp 100% ghi chú spine. |
| Java 21 với SB 4.x | **Hợp lệ.** Migration guide chính thức: "Spring Boot 4.0 requires Java 17 or later". Initializr cho chọn Java **17 (default) / 21 / 25 / 27** — đã sinh thành công project thật SB 4.1.1 + Java 21. Jakarta EE 11 / Servlet 6.1 baseline. |
| Initializr default | `bootVersion` default = **4.1.1**, `javaVersion` default = **17** → nhóm phải chủ động chọn 21 khi init. |

Nguồn:
- https://repo1.maven.org/maven2/org/springframework/boot/spring-boot/maven-metadata.xml
- https://endoflife.date/spring-boot
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://start.spring.io/metadata/client

## 2. springdoc-openapi — VERIFIED (spine ghi "verify khi init")

- **springdoc-openapi 3.1.1 là bản mới nhất** (Maven Central, lastUpdated 2026-09-06). Dòng **3.x = line cho Spring Boot 4** ("Spring-boot v4 (Java 17 & Jakarta EE 9)"); 2.9.1 là cuối dòng 2.x cho SB 3.x; 1.x cho SB 2.x.
- Spine có thể thay "tương thích SB 4.x — verify khi init repo" bằng con số cụ thể: **`springdoc-openapi-starter-webmvc-ui` 3.1.1**.

Nguồn:
- https://repo1.maven.org/maven2/org/springdoc/springdoc-openapi-starter-webmvc-ui/maven-metadata.xml
- https://springdoc.org/

## 3. React 19.3 / Vite 8.3 — CONFIRMED

- npm dist-tags: **react `latest` = 19.3.0** (canary cũng 19.3.x) — đúng spine.
- **vite `latest` = 8.3.0**, `previous` = 7.3.6 — đúng spine.
- Template chính thống đối chiếu: `create-vite` 9.2.1, template `react-ts` ship `react ^19.2.8` + `vite ^8.3.0` + `@vitejs/plugin-react ^6.1.1` — cùng major/minor với spine, không lệch.

Nguồn:
- https://registry.npmjs.org/-/package/react/dist-tags
- https://registry.npmjs.org/-/package/vite/dist-tags
- https://registry.npmjs.org/create-vite/-/create-vite-9.2.1.tgz (template `package/template-react-ts/package.json`)

## 4. MySQL 8.4 LTS — CONFIRMED

- 8.4 LTS vẫn được maintain: premier support đến **30-04-2029**, extended đến **30-04-2032**.
- Patch mới nhất của dòng 8.4 = **8.4.11** (2026-06-30) — đúng spine, chưa có 8.4.12.
- Tham khảo thêm: đã có MySQL **9.7 LTS** (2026-04-21, patch 9.7.2) — spine không cần đổi, nhưng nếu môi trường demo có sẵn 9.x thì driver trong Boot (Connector/J 9.7.0) vẫn chạy tốt cả 8.4 lẫn 9.x.

Nguồn:
- https://endoflife.date/mysql
- BOM SB 4.1.1 manage `mysql.version = 9.7.0` (Connector/J): https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom

## 5. Thư viện kèm theo — tồn tại & phù hợp, 2 nhãn version cần chỉnh

| Thư viện | Hiện trạng (2026-09-22) | Phù hợp? |
| --- | --- | --- |
| TanStack Query | `@tanstack/react-query` latest **5.103.2**; peerDeps `react: ^18 \|\| ^19` | Có — chạy được với React 19.3 |
| Axios | latest **1.20.0** | Có |
| MSW | latest **2.15.0** (dòng 2.x là current; có backport 1.3.5) | Có — chú ý dùng docs MSW **2.x**, API `http` handler khác MSW 1.x |
| JUnit | BOM SB 4.1.1 manage **junit-jupiter 6.0.3** — tức dòng **JUnit 6**, không phải JUnit 5 | Có, nhưng **spine đang ghi "JUnit 5" — sai nhãn version** |
| Mockito | **5.23.0** (BOM) | Có |
| AssertJ | **3.27.7** (BOM) | Có |

Chi tiết BOM SB 4.1.1 khác (để tham khảo): Spring Security **7.1.1**, Spring Framework **7.0.9**, Hibernate **7.4.5.Final**, Flyway **12.4.0**, Tomcat **11.0.24**, Jakarta Validation **3.1.1**.

Nguồn:
- https://registry.npmjs.org/-/package/@tanstack%2Freact-query/dist-tags và https://registry.npmjs.org/@tanstack/react-query/latest (peerDependencies)
- https://registry.npmjs.org/-/package/axios/dist-tags · https://registry.npmjs.org/-/package/msw/dist-tags
- https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.1/spring-boot-dependencies-4.1.1.pom

## 6. Starter defaults — những thay đổi so với "quan niệm phổ biến" (quan trọng cho 4 sinh viên)

Đã **sinh project thật** trên start.spring.io (SB 4.1.1 · Java 21 · Maven · deps web, data-jpa, security, validation, flyway, mysql) ngày 2026-09-22. Kết quả:

1. **`spring-boot-starter-web` đã bị đổi tên → `spring-boot-starter-webmvc`.** Initializr phát sinh `spring-boot-starter-webmvc`; migration guide xác nhận starter cũ "deprecated, sẽ bị gỡ trong tương lai" (tương tự `oauth2-client` → `security-oauth2-client`). Tutorial/sample code older sẽ ghi `starter-web` — nhóm cần biết hai tên này.
2. **Test starter tách theo module**: sinh ra `spring-boot-starter-webmvc-test`, `-data-jpa-test`, `-security-test`, `-validation-test`, `-flyway-test` (mỗi cái kéo kèm `spring-boot-starter-test` transitively — không cần khai riêng nữa). Đặc biệt: **`@WithMockUser`/`@WithUserDetails` giờ cần `spring-boot-starter-security-test`**.
3. **Flyway**: Initializr tự thêm `spring-boot-starter-flyway` + **`flyway-mysql`** (plugin DB riêng cho MySQL, bắt buộc từ Flyway 10+) + `mysql-connector-j`. Vị trí migration mặc định `db/migration` không đổi — khớp AD-6 của spine.
4. **Jackson 3 là mặc định trong Boot 4 / Security 7** (BOM manage `jackson-bom 3.1.5`, nhóm `tools.jackson`; Jackson 2 chỉ còn BOM legacy 2.21.5). Spring Security 7 migration: `SecurityJackson2Modules` → `SecurityJacksonModules`. Code mẫu trên mạng dùng `com.fasterxml.jackson...` cho mục đích security-internal có thể không còn đúng — với DTO thường của StorageHub thì ảnh hưởng thấp, nhưng đáng biết.
5. **Initializr KHÔNG sinh sẵn SecurityConfiguration** — file sinh ra chỉ có `BackendApplication`, test `@SpringBootTest contextLoads()` (dùng `org.junit.jupiter.api.Test` — API Jupiter giữ nguyên), `application.properties` chỉ chứa `spring.application.name`. Nghĩa là mọi cấu hình JWT/BCrypt theo AD-5 nhóm tự viết từ đầu, như spine đã định.
6. Default Java của Initializr là **17**, không phải 21 — phải chủ động chọn 21 khi init (hoặc sửa `java.version` trong pom).

Nguồn:
- Project sinh thật: https://start.spring.io/starter.zip?type=maven-project&language=java&bootVersion=4.1.1&javaVersion=21&dependencies=web,data-jpa,security,validation,flyway,mysql (truy vấn 2026-09-22)
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Release-Notes
- https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.1-Release-Notes (Flyway 12.4.0 chỉ là upgrade, không đổi behavior; không đổi security defaults)
- https://docs.spring.io/spring-security/reference/7.0/migration/

## 7. TypeScript — cần cập nhật assumption

- Spine ghi "TypeScript 5.x `[ASSUMPTION]`". Thực tế 2026-09-22: **TS stable mới nhất = 7.0.2** (dòng 5.x đã cũ; template `react-ts` của create-vite 9.2.1 pin `typescript ~6.0.2`).
- Không cần quyết gì thêm (spine đã defer TS vs JS) — nhưng khi đọc lại spine, ghi "5.x" sẽ gây hiểu sai về những gì `npm create vite` sẽ cài. Đề xuất ghi "TypeScript — theo template create-vite hiện hành (~6.0.x; latest 7.x)" hoặc để "current theo template".

Nguồn: https://registry.npmjs.org/-/package/typescript/dist-tags · template create-vite 9.2.1 (mục 3)

## Tổng hợp findings (theo severity)

| # | Severity | Finding | Đề xuất (không sửa spine trong review này) |
| --- | --- | --- | --- |
| F1 | Medium | Spine để springdoc "verify khi init" — đã verify được: **3.1.1** hỗ trợ SB 4.x | Ghi cứng `springdoc-openapi-starter-webmvc-ui 3.1.1` vào bảng Stack |
| F2 | Medium | Spine ghi "JUnit 5" nhưng SB 4.1.1 bundle **JUnit Jupiter 6.0.3** (JUnit 6) | Sửa nhãn thành "JUnit 6 (Jupiter) + Mockito 5.23 + AssertJ 3.27 — bundle Spring Boot" |
| F3 | Medium | Starter defaults Boot 4 khác tutorial cũ: `web`→`webmvc`, test starter tách module, `security-test` cần cho `@WithMockUser`, Jackson 3 mặc định | Thêm 3–5 dòng "Boot 4 gotchas" vào khi init repo / onboarding (không phải lỗi spine — spine không khai báo tên starter) |
| F4 | Low | TypeScript spine "5.x" đã lỗi thời — hiện stable 7.0.2, template Vite dùng ~6.0.2 | Cập nhật dòng TypeScript trong Stack theo mục 7 |
| F5 | Low | Initializr default Java = 17 (không phải 21); đã có MySQL 9.7 LTS song song 8.4 | Chủ động chọn 21 khi init; giữ 8.4.11 là hợp lý (LTS đến 2032) |

## Kết luận

Không có decision nào trong spine bị phản bác bởi hiện trạng web ngày 2026-09-22. Bốn version chủ chốt (SB 4.1.1, React 19.3.0, Vite 8.3.0, MySQL 8.4.11) đều là stable/mới nhất của dòng đang chọn; support window đủ dài cho khóa học (SB 4.1 OSS đến 2027-07, MySQL 8.4 đến 2032). Các finding đều là tinh chỉnh nhãn version và ghi chú onboarding, không đổi architecture decision nào (AD-1…AD-9 không ảnh hưởng).
