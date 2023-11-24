# Change Log

## [5.12.0](https://github.com/joffrey-bion/krossbow/tree/5.12.0) (2023-11-24)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.11.0...5.12.0)

**Upgraded dependencies:**

- Bump com.fasterxml.jackson:jackson\-bom from 2.15.3 to 2.16.0 [\#405](https://github.com/joffrey-bion/krossbow/pull/405) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump kotlinx\-serialization from 1.6.0 to 1.6.1 [\#404](https://github.com/joffrey-bion/krossbow/pull/404) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump kotlin from 1.9.20 to 1.9.21 [\#409](https://github.com/joffrey-bion/krossbow/pull/409) ([@dependabot[bot]](https://github.com/apps/dependabot))

## [5.11.0](https://github.com/joffrey-bion/krossbow/tree/5.11.0) (2023-11-14)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.10.0...5.11.0)

**Upgraded dependencies:**

- Bump com.benasher44:uuid from 0.8.1 to 0.8.2 [\#403](https://github.com/joffrey-bion/krossbow/pull/403) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump kotlin from 1.9.10 to 1.9.20 [\#402](https://github.com/joffrey-bion/krossbow/issues/402)
- Bump ktor from 2.3.5 to 2.3.6 [\#401](https://github.com/joffrey-bion/krossbow/pull/401) ([@dependabot[bot]](https://github.com/apps/dependabot))

## [5.10.0](https://github.com/joffrey-bion/krossbow/tree/5.10.0) (2023-11-02)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.8.0...5.10.0)

**Upgraded dependencies:**

- Bump com.squareup.okhttp3:okhttp from 4.11.0 to 4.12.0 [\#396](https://github.com/joffrey-bion/krossbow/pull/396) ([@dependabot[bot]](https://github.com/apps/dependabot))

## [5.8.0](https://github.com/joffrey-bion/krossbow/tree/5.8.0) (2023-10-14)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.7.0...5.8.0)

**Upgraded dependencies:**

- Bump com.fasterxml.jackson:jackson\-bom from 2.15.2 to 2.15.3 [\#393](https://github.com/joffrey-bion/krossbow/pull/393) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump ktor from 2.3.4 to 2.3.5 [\#389](https://github.com/joffrey-bion/krossbow/pull/389) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump org.springframework:spring\-websocket from 6.0.12 to 6.0.13 [\#392](https://github.com/joffrey-bion/krossbow/pull/392) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump com.squareup.okio:okio from 3.5.0 to 3.6.0 [\#387](https://github.com/joffrey-bion/krossbow/pull/387) ([@dependabot[bot]](https://github.com/apps/dependabot))

## [5.7.0](https://github.com/joffrey-bion/krossbow/tree/5.7.0) (2023-09-19)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.6.0...5.7.0)

**Implemented enhancements:**

- `asKrossbowWebSocketClient\(\)` extension to adapt Spring clients to Krossbow WS API [\#383](https://github.com/joffrey-bion/krossbow/issues/383)

**Deprecations:**

- Deprecate built\-in adapted Spring client objects in favor of `asKrossbowWebSocketClient\(\)` extension [\#385](https://github.com/joffrey-bion/krossbow/issues/385)
- Deprecate `SpringWebSocketClientAdapter` in favor of `asKrossbowWebSocketClient\(\)` extension [\#384](https://github.com/joffrey-bion/krossbow/issues/384)
- Deprecate Spring Jetty client \(deprecated in Spring 6 for removal\) [\#382](https://github.com/joffrey-bion/krossbow/issues/382)

**Upgraded dependencies:**

- Upgrade Spring to version 6.0.12 \(Java 17\+ only\) [\#298](https://github.com/joffrey-bion/krossbow/issues/298)

## [5.6.0](https://github.com/joffrey-bion/krossbow/tree/5.6.0) (2023-09-18)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.5.0...5.6.0)

**Implemented enhancements:**

- Define a specific `SessionDisconnectedException` to represent STOMP frames completion [\#379](https://github.com/joffrey-bion/krossbow/issues/379)

**Upgraded dependencies:**

- Bump org.eclipse.jetty.websocket:websocket\-client from 9.4.51.v20230217 to 9.4.52.v20230823 [\#373](https://github.com/joffrey-bion/krossbow/pull/373) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump com.benasher44:uuid from 0.8.0 to 0.8.1 [\#375](https://github.com/joffrey-bion/krossbow/pull/375) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump ktor from 2.3.3 to 2.3.4 [\#371](https://github.com/joffrey-bion/krossbow/pull/371) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump kotlin from 1.9.0 to 1.9.10 [\#368](https://github.com/joffrey-bion/krossbow/pull/368) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump kotlinx\-serialization from 1.5.1 to 1.6.0 [\#367](https://github.com/joffrey-bion/krossbow/pull/367) ([@dependabot[bot]](https://github.com/apps/dependabot))

## [5.5.0](https://github.com/joffrey-bion/krossbow/tree/5.5.0) (2023-08-21)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.4.0...5.5.0)

**Implemented enhancements:**

- Support for custom headers in the web socket handshake [\#360](https://github.com/joffrey-bion/krossbow/issues/360)

**Upgraded dependencies:**

- Bump org.jetbrains.kotlinx:atomicfu from 0.21.0 to 0.22.0 [\#362](https://github.com/joffrey-bion/krossbow/pull/362) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Upgrade uuid to version 0.8.0 [\#361](https://github.com/joffrey-bion/krossbow/issues/361)
- Upgrade Okio to version 3.5.0 [\#358](https://github.com/joffrey-bion/krossbow/issues/358)
- Upgrade Kotlin to version 1.9.0 [\#357](https://github.com/joffrey-bion/krossbow/issues/357)

**Fixed bugs:**

- OkHttp fails to report handshake failures when it can't read the response body [\#359](https://github.com/joffrey-bion/krossbow/issues/359)

## [5.4.0](https://github.com/joffrey-bion/krossbow/tree/5.4.0) (2023-08-09)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.3.0...5.4.0)

**Upgraded dependencies:**

- Bump ktor from 2.3.1 to 2.3.3 [\#351](https://github.com/joffrey-bion/krossbow/pull/351) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump org.jetbrains.kotlinx:atomicfu from 0.20.2 to 0.21.0 [\#353](https://github.com/joffrey-bion/krossbow/pull/353) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump kotlinx\-coroutines from 1.7.1 to 1.7.3 [\#349](https://github.com/joffrey-bion/krossbow/pull/349) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Bump com.squareup.okio:okio from 3.3.0 to 3.4.0 [\#342](https://github.com/joffrey-bion/krossbow/pull/342) ([@dependabot[bot]](https://github.com/apps/dependabot))

**Fixed bugs:**

- Cannot Assemble XCFramework with Krossbow Dependencies in a Multiplatform Project [\#356](https://github.com/joffrey-bion/krossbow/issues/356)

## [5.3.0](https://github.com/joffrey-bion/krossbow/tree/5.3.0) (2023-07-03)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.2.0...5.3.0)

**Implemented enhancements:**

- Provide additional info in `WebSocketConnectionException` [\#337](https://github.com/joffrey-bion/krossbow/issues/337)

**Removals:**

- Remove support for deprecated `watchosX86` target [\#336](https://github.com/joffrey-bion/krossbow/issues/336)

**Upgraded dependencies:**

- Upgrade Kotlin to version 1.8.22 [\#335](https://github.com/joffrey-bion/krossbow/issues/335)
- Upgrade jackson to version 2.15.2 [\#334](https://github.com/joffrey-bion/krossbow/issues/334)
- Upgrade uuid to version 0.7.1 [\#333](https://github.com/joffrey-bion/krossbow/issues/333)
- Upgrade Ktor to version 2.3.1 [\#332](https://github.com/joffrey-bion/krossbow/issues/332)

## [5.2.0](https://github.com/joffrey-bion/krossbow/tree/5.2.0) (2023-05-16)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.1.0...5.2.0)

**Implemented enhancements:**

- Allow to override coroutine name and job in stomp session context [\#308](https://github.com/joffrey-bion/krossbow/issues/308)

**Removals:**

- IR\-only: stop publishing JS artifacts compiled with the legacy compiler [\#290](https://github.com/joffrey-bion/krossbow/issues/290)
- Remove support for legacy non\-hierarchical projects \(`enableCompatibilityMetadataVariant`\) [\#313](https://github.com/joffrey-bion/krossbow/issues/313)

**Upgraded dependencies:**

- Upgrade slf4j to version 2.0.7 [\#330](https://github.com/joffrey-bion/krossbow/issues/330)
- Upgrade OkHttp to version 4.11.0 [\#329](https://github.com/joffrey-bion/krossbow/issues/329)
- Upgrade Moshi to version 1.15.0 [\#328](https://github.com/joffrey-bion/krossbow/issues/328)
- Upgrade jackson to version 2.15.0 [\#327](https://github.com/joffrey-bion/krossbow/issues/327)
- Upgrade kotlinx\-serialization to version 1.5.1 [\#326](https://github.com/joffrey-bion/krossbow/issues/326)
- Upgrade kotlinx\-coroutines to version 1.7.1 [\#325](https://github.com/joffrey-bion/krossbow/issues/325)
- Upgrade kotlinx\-atomicfu to version 0.20.2 [\#317](https://github.com/joffrey-bion/krossbow/issues/317)
- Upgrade Kotlin to version 1.8.21 [\#316](https://github.com/joffrey-bion/krossbow/issues/316)
- Upgrade Ktor to version 2.3.0 [\#312](https://github.com/joffrey-bion/krossbow/issues/312)
- Upgrade Kotlin to version 1.8.20 [\#309](https://github.com/joffrey-bion/krossbow/issues/309)

## [5.1.0](https://github.com/joffrey-bion/krossbow/tree/5.1.0) (2023-03-05)
[View commits](https://github.com/joffrey-bion/krossbow/compare/5.0.0...5.1.0)

**Upgraded dependencies:**

- Upgrade uuid to version 0.7.0 [\#307](https://github.com/joffrey-bion/krossbow/issues/307)
- Upgrade Ktor to version 2.2.4 [\#306](https://github.com/joffrey-bion/krossbow/issues/306)
- Upgrade kotlinx\-serialization to version 1.5.0 [\#305](https://github.com/joffrey-bion/krossbow/issues/305)
- Upgrade jetty\-websocket to version 9.4.51.v20230217 [\#304](https://github.com/joffrey-bion/krossbow/issues/304)
- Upgrade jackson to version 2.14.2 [\#303](https://github.com/joffrey-bion/krossbow/issues/303)
- Upgrade Kotlin to version 1.8.10 [\#302](https://github.com/joffrey-bion/krossbow/issues/302)

## [5.0.0](https://github.com/joffrey-bion/krossbow/tree/5.0.0) (2023-01-16)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.5.0...5.0.0)

**Removals:**

- Drop support for Ktor 1.x \(legacy\) because incompatible with Kotlin 1.8 [\#296](https://github.com/joffrey-bion/krossbow/issues/296)

**Upgraded dependencies:**

- Upgrade jetty\-websocket to version 9.4.50.v20221201 [\#297](https://github.com/joffrey-bion/krossbow/issues/297)
- Upgrade uuid to version 0.6.0 [\#295](https://github.com/joffrey-bion/krossbow/issues/295)
- Upgrade okio to version 3.3.0 \(depends on Kotlin 1.8\) [\#294](https://github.com/joffrey-bion/krossbow/issues/294)
- Upgrade Jackson to version 2.14.1 [\#292](https://github.com/joffrey-bion/krossbow/issues/292)
- Upgrade kotlinx\-atomicfu to version 0.19.0 [\#293](https://github.com/joffrey-bion/krossbow/issues/293)
- Upgrade Kotlin to version 1.8.0 [\#289](https://github.com/joffrey-bion/krossbow/issues/289)
- Upgrade Ktor to version 2.2.2 [\#291](https://github.com/joffrey-bion/krossbow/issues/291)

**Fixed bugs:**

- Compilation of gradle Java 11 project failed [\#288](https://github.com/joffrey-bion/krossbow/issues/288)

## [4.5.0](https://github.com/joffrey-bion/krossbow/tree/4.5.0) (2022-11-26)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.4.0...4.5.0)

**Upgraded dependencies:**

- Upgrade Kotlin to version 1.7.21 [\#283](https://github.com/joffrey-bion/krossbow/issues/283)

## [4.4.0](https://github.com/joffrey-bion/krossbow/tree/4.4.0) (2022-10-29)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.3.0...4.4.0)

**Upgraded dependencies:**

- Upgrade kotlinx\-atomicfu to version 0.18.5 [\#279](https://github.com/joffrey-bion/krossbow/issues/279)
- Upgrade Ktor to version 2.1.3 [\#278](https://github.com/joffrey-bion/krossbow/issues/278)

## [4.3.0](https://github.com/joffrey-bion/krossbow/tree/4.3.0) (2022-10-24)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.2.0...4.3.0)

**Upgraded dependencies:**

- Upgrade transitive jackson\-databind to version 2.13.4.2 via Jackson BOM 2.13.4.20221013 [\#277](https://github.com/joffrey-bion/krossbow/issues/277)
- Upgrade kotlinx\-serialization to version 1.4.1 [\#276](https://github.com/joffrey-bion/krossbow/issues/276)

## [4.2.0](https://github.com/joffrey-bion/krossbow/tree/4.2.0) (2022-10-18)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.1.0...4.2.0)

**Implemented enhancements:**

- Add support for `watchosX64` target [\#228](https://github.com/joffrey-bion/krossbow/issues/228)

**Upgraded dependencies:**

- Upgrade Kotlin to version 1.7.20 [\#274](https://github.com/joffrey-bion/krossbow/issues/274)
- Upgrade okhttp to version 4.10.0 [\#272](https://github.com/joffrey-bion/krossbow/issues/272)
- Upgrade Jackson to version 2.13.4 [\#264](https://github.com/joffrey-bion/krossbow/issues/264)
- Upgrade Spring to version 5.3.23 [\#271](https://github.com/joffrey-bion/krossbow/issues/271)
- Upgrade Moshi to version 1.14.0 [\#270](https://github.com/joffrey-bion/krossbow/issues/270)
- Upgrade Ktor to version 2.1.2 [\#269](https://github.com/joffrey-bion/krossbow/issues/269)
- Upgrade kotlinx\-serialization to version 1.4.0 [\#268](https://github.com/joffrey-bion/krossbow/issues/268)
- Upgrade kotlinx\-atomicfu to version 0.18.4 [\#267](https://github.com/joffrey-bion/krossbow/issues/267)
- Upgrade jetty\-websocket to version 9.4.49.v20220914 [\#266](https://github.com/joffrey-bion/krossbow/issues/266)
- Upgrade Kotlinx Coroutines to version 1.6.4 [\#265](https://github.com/joffrey-bion/krossbow/issues/265)
- Upgrade uuid to version 0.5.0 [\#263](https://github.com/joffrey-bion/krossbow/issues/263)
- Upgrade Dokka to version 1.7.10 [\#262](https://github.com/joffrey-bion/krossbow/issues/262)
- Upgrade Ktor to version 2.1.1 [\#260](https://github.com/joffrey-bion/krossbow/issues/260)
- Upgrade Okio to version 3.2.0 [\#224](https://github.com/joffrey-bion/krossbow/issues/224)

## [4.1.0](https://github.com/joffrey-bion/krossbow/tree/4.1.0) (2022-07-22)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.0.3...4.1.0)

**Upgraded dependencies:**

- Upgrade Ktor to version 2.0.3 [\#256](https://github.com/joffrey-bion/krossbow/issues/256)
- Upgrade Kotlin to version 1.7.10 [\#253](https://github.com/joffrey-bion/krossbow/issues/253)

## [4.0.3](https://github.com/joffrey-bion/krossbow/tree/4.0.3) (2022-07-04)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.0.2...4.0.3)

**Implemented enhancements:**

- `Exception`\-\>`Throwable` parameter for `ReconnectConfig.shouldReconnect` [\#249](https://github.com/joffrey-bion/krossbow/issues/249)
- Make cause non\-nullable for `WebSocketReconnectionException` [\#248](https://github.com/joffrey-bion/krossbow/issues/248)

**Deprecations:**

- Deprecate `WebSocketClient.default\(\)` in favor of `builtIn\(\)` [\#250](https://github.com/joffrey-bion/krossbow/issues/250)

**Fixed bugs:**

- `NoSuchMethodError` when using the `StompClient\(\)` factory function without WS client [\#251](https://github.com/joffrey-bion/krossbow/issues/251)

## [4.0.2](https://github.com/joffrey-bion/krossbow/tree/4.0.2) (2022-06-24)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.0.1...4.0.2)

**Upgraded dependencies:**

- Upgrade Dokka to version 1.7.0 [\#244](https://github.com/joffrey-bion/krossbow/issues/244)

**Fixed bugs:**

- NoClassDefFoundError: WebSocketHandshakeException [\#246](https://github.com/joffrey-bion/krossbow/issues/246)

## [4.0.1](https://github.com/joffrey-bion/krossbow/tree/4.0.1) (2022-06-21)
[View commits](https://github.com/joffrey-bion/krossbow/compare/4.0.0...4.0.1)

**Upgraded dependencies:**

- Upgrade spring\-websocket to version 5.3.21 [\#243](https://github.com/joffrey-bion/krossbow/issues/243)

## [4.0.0](https://github.com/joffrey-bion/krossbow/tree/4.0.0) (2022-06-20)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.8.0...4.0.0)

**Breaking changes:**

- Reorganize modules to support all targets in pure Kotlin modules [\#234](https://github.com/joffrey-bion/krossbow/issues/234)

**Removals:**

- Remove deprecated type alias `WebSocketSession` [\#240](https://github.com/joffrey-bion/krossbow/issues/240)
- Remove deprecated StompSessionWithClassConversions and related code [\#239](https://github.com/joffrey-bion/krossbow/issues/239)
- Remove `compileOnly\(kotlinx\-serialization\-json\)` dependency from `krossbow\-stomp\-kxserialization` [\#237](https://github.com/joffrey-bion/krossbow/issues/237)

**Upgraded dependencies:**

- Upgrade Kotlinx Coroutines to version 1.6.3 [\#219](https://github.com/joffrey-bion/krossbow/issues/219)
- Upgrade Kotlin to version 1.7.0 [\#233](https://github.com/joffrey-bion/krossbow/issues/233)

## [3.8.0](https://github.com/joffrey-bion/krossbow/tree/3.8.0) (2022-06-11)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.7.0...3.8.0)

**Deprecations:**

- Move `withJsonConversions` to its own module `krossbow\-stomp\-kxserialization\-json` [\#238](https://github.com/joffrey-bion/krossbow/issues/238)

## [3.7.0](https://github.com/joffrey-bion/krossbow/tree/3.7.0) (2022-06-09)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.6.0...3.7.0)

**Implemented enhancements:**

- Add support for `watchosSimulatorArm64` and `tvosSimulatorArm64` targets [\#227](https://github.com/joffrey-bion/krossbow/issues/227)

**Upgraded dependencies:**

- Bump actions/setup\-python from 3 to 4 [\#232](https://github.com/joffrey-bion/krossbow/pull/232) ([@dependabot[bot]](https://github.com/apps/dependabot))
- Upgrade uuid to version 0.4.1 \(which got support for watchosX64 target\) [\#230](https://github.com/joffrey-bion/krossbow/issues/230)
- Upgrade Dokka to version 1.6.21 [\#229](https://github.com/joffrey-bion/krossbow/issues/229)

## [3.6.0](https://github.com/joffrey-bion/krossbow/tree/3.6.0) (2022-05-31)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.5.0...3.6.0)

**Upgraded dependencies:**

- Upgrade jetty\-websocket to version 9.4.46.v20220331 [\#221](https://github.com/joffrey-bion/krossbow/issues/221)
- Upgrade Jackson to version 2.13.3 [\#220](https://github.com/joffrey-bion/krossbow/issues/220)
- Upgrade Spring to version 5.3.20 [\#225](https://github.com/joffrey-bion/krossbow/issues/225)
- Upgrade Ktor to version 2.0.2 [\#223](https://github.com/joffrey-bion/krossbow/issues/223)
- Upgrade kotlinx\-serialization to version 1.3.3 [\#222](https://github.com/joffrey-bion/krossbow/issues/222)
- Upgrade Atomicfu to version 0.17.3 [\#218](https://github.com/joffrey-bion/krossbow/issues/218)

**Fixed bugs:**

- `krossbow\-stomp\-moshi` missing in Maven Central [\#226](https://github.com/joffrey-bion/krossbow/issues/226)

## [3.5.0](https://github.com/joffrey-bion/krossbow/tree/3.5.0) (2022-05-31)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.4.0...3.5.0)

**Implemented enhancements:**

- Add support for JSON conversions with Moshi [\#206](https://github.com/joffrey-bion/krossbow/issues/206)
- New `TypedStompSession` which handles full generic types [\#214](https://github.com/joffrey-bion/krossbow/issues/214)
- Add HTTP status code to WebSocketConnectionException [\#210](https://github.com/joffrey-bion/krossbow/issues/210)

**Deprecations:**

- Deprecate `StompSessionWithClassConversions` in favor of the new `TypedStompSession` [\#215](https://github.com/joffrey-bion/krossbow/issues/215)

**Merged pull requests:**

- Include NSError code, domain and HTTP Status code in DarwinWebSocketException message [\#209](https://github.com/joffrey-bion/krossbow/pull/209) ([@remy-bardou-lifeonair](https://github.com/remy-bardou-lifeonair))

**Upgraded dependencies:**

- Upgrade sockjs\-client to version 1.6.1 [\#217](https://github.com/joffrey-bion/krossbow/issues/217)

**Fixed bugs:**

- `withTransaction` ignores 2nd exception if `abort` throws [\#216](https://github.com/joffrey-bion/krossbow/issues/216)
- Jackson deserialization doesn't work on generic types [\#212](https://github.com/joffrey-bion/krossbow/issues/212)

## [3.4.0](https://github.com/joffrey-bion/krossbow/tree/3.4.0) (2022-04-22)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.3.1...3.4.0)

**Implemented enhancements:**

- Release Kotlin/JS artifacts built with IR compiler [\#98](https://github.com/joffrey-bion/krossbow/issues/98)
- Support Ktor client 2.0 [\#186](https://github.com/joffrey-bion/krossbow/issues/186)

**Upgraded dependencies:**

- Upgrade Kotlin coroutines to version 1.6.1 [\#203](https://github.com/joffrey-bion/krossbow/issues/203)
- Upgrade Kotlin to version 1.6.21 [\#184](https://github.com/joffrey-bion/krossbow/issues/184)

## [3.3.1](https://github.com/joffrey-bion/krossbow/tree/3.3.1) (2022-04-01)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.3.0...3.3.1)

**Fixed bugs:**

- Mark `NO\_HEART\_BEATS` as `@SharedImmutable` to fix `IncorrectDereferenceException` on iOS  [\#202](https://github.com/joffrey-bion/krossbow/pull/202) ([@remy-bardou-lifeonair](https://github.com/remy-bardou-lifeonair))

## [3.3.0](https://github.com/joffrey-bion/krossbow/tree/3.3.0) (2022-03-29)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.2.0...3.3.0)

**Upgraded dependencies:**

- Upgrade spring\-websocket to version 5.3.17 [\#200](https://github.com/joffrey-bion/krossbow/issues/200)
- Upgrade kotlinx\-atomicfu to version 0.17.1 [\#199](https://github.com/joffrey-bion/krossbow/issues/199)
- Upgrade sockjs\-client to version 1.6.0 [\#198](https://github.com/joffrey-bion/krossbow/issues/198)
- Upgrade jackson\-core to version 2.13.2 [\#197](https://github.com/joffrey-bion/krossbow/issues/197)
- Upgrade uuid to version 0.4.0 [\#195](https://github.com/joffrey-bion/krossbow/issues/195)
- Upgrade Ktor to version 1.6.8 [\#194](https://github.com/joffrey-bion/krossbow/issues/194)

## [3.2.0](https://github.com/joffrey-bion/krossbow/tree/3.2.0) (2022-03-28)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.1.1...3.2.0)

**Implemented enhancements:**

- watchOS/tvOS support for `krossbow\-stomp\-kxserialization` [\#193](https://github.com/joffrey-bion/krossbow/issues/193)

**Merged pull requests:**

- Add iOS ARM64 Simulator Support \(M1\) [\#192](https://github.com/joffrey-bion/krossbow/pull/192) ([@remy-bardou-lifeonair](https://github.com/remy-bardou-lifeonair))

## [3.1.1](https://github.com/joffrey-bion/krossbow/tree/3.1.1) (2022-01-16)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.1.0...3.1.1)

**Fixed bugs:**

- Software caused connection abort on network disconnection [\#173](https://github.com/joffrey-bion/krossbow/issues/173)

## [3.1.0](https://github.com/joffrey-bion/krossbow/tree/3.1.0) (2021-12-31)
[View commits](https://github.com/joffrey-bion/krossbow/compare/3.0.0...3.1.0)

**Implemented enhancements:**

- Add support for `watchos` and `tvos` targets [\#158](https://github.com/joffrey-bion/krossbow/issues/158)

## [3.0.0](https://github.com/joffrey-bion/krossbow/tree/3.0.0) (2021-12-27)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.7.0...3.0.0)

**Breaking changes:**

- Use Duration instead of long millis in Stomp configuration [\#169](https://github.com/joffrey-bion/krossbow/issues/169)
- Use Flow instead of Channel in WebSocketConnection API  [\#120](https://github.com/joffrey-bion/krossbow/issues/120)

**Implemented enhancements:**

- Switch iOS to new K/N memory model [\#183](https://github.com/joffrey-bion/krossbow/issues/183)
- Conditional retry [\#172](https://github.com/joffrey-bion/krossbow/issues/172)

**Removals:**

- Hide internal Charsets APIs [\#171](https://github.com/joffrey-bion/krossbow/issues/171)
- Remove the deprecated `defaultWebSocketClient\(\)` top\-level function [\#166](https://github.com/joffrey-bion/krossbow/issues/166)
- Stop embedding Tyrus implementation of JSR\-356 with `krossbow\-websocket\-spring` [\#135](https://github.com/joffrey-bion/krossbow/issues/135)

**Upgraded dependencies:**

- Upgrade OkHttp to version 4.9.3 [\#182](https://github.com/joffrey-bion/krossbow/issues/182)
- Upgrade Jetty websocket client to version 9.4.44.v20210927 [\#181](https://github.com/joffrey-bion/krossbow/issues/181)
- Upgrade Spring Websocket to version 5.3.14 [\#180](https://github.com/joffrey-bion/krossbow/issues/180)
- Upgrade Jackson to version 2.13.1 [\#179](https://github.com/joffrey-bion/krossbow/issues/179)
- Upgrade Kotlinx Serialization to version 1.3.2 [\#178](https://github.com/joffrey-bion/krossbow/issues/178)
- Upgrade sockjs\-client to version 1.5.2 [\#177](https://github.com/joffrey-bion/krossbow/issues/177)
- Upgrade Ktor to version 1.6.7 [\#176](https://github.com/joffrey-bion/krossbow/issues/176)
- Upgrade Kotlin coroutines to version 1.6.0 [\#167](https://github.com/joffrey-bion/krossbow/issues/167)
- Upgrade Kotlin to version 1.6.10 [\#175](https://github.com/joffrey-bion/krossbow/issues/175)
- Upgrade Dokka to version 1.6.0 [\#170](https://github.com/joffrey-bion/krossbow/issues/170)
- Upgrade atomicfu to version 0.17.0 \(uses Kotlin 1.6\) [\#168](https://github.com/joffrey-bion/krossbow/issues/168)
- Upgrade Okio to version 3.0.0 [\#164](https://github.com/joffrey-bion/krossbow/issues/164)

**Fixed bugs:**

- Reconnecting web socket doesn't close normally its incomingFrames flow [\#174](https://github.com/joffrey-bion/krossbow/issues/174)
- `StompClient.connect\(\)` might leak the WS connection if cancelled just before STOMP handshake [\#163](https://github.com/joffrey-bion/krossbow/issues/163)
- Ktor client errors are not wrapped in Krossbow's WebSocketException [\#160](https://github.com/joffrey-bion/krossbow/issues/160)

## [2.7.0](https://github.com/joffrey-bion/krossbow/tree/2.7.0) (2021-10-11)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.6.0...2.7.0)

**Upgraded dependencies:**

- Upgrade Kotlinx Serialization to version 1.3.0 [\#159](https://github.com/joffrey-bion/krossbow/issues/159)

## [2.6.0](https://github.com/joffrey-bion/krossbow/tree/2.6.0) (2021-10-10)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.5.2...2.6.0)

**Implemented enhancements:**

- iOS support  [\#126](https://github.com/joffrey-bion/krossbow/issues/126)

**Fixed bugs:**

- Ktor web socket client doesn't report Close frames? [\#141](https://github.com/joffrey-bion/krossbow/issues/141)

## [2.5.2](https://github.com/joffrey-bion/krossbow/tree/2.5.2) (2021-10-08)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.5.1...2.5.2)

**Fixed bugs:**

- `connect\(\)` doesn't fail on connection failure with OkHttp client [\#138](https://github.com/joffrey-bion/krossbow/issues/138)

## [2.5.1](https://github.com/joffrey-bion/krossbow/tree/2.5.1) (2021-10-08)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.5.0...2.5.1)

**Upgraded dependencies:**

- Upgrade Dokka to version 1.5.30 [\#152](https://github.com/joffrey-bion/krossbow/issues/152)
- Upgrade Kotlin to version 1.5.31 [\#151](https://github.com/joffrey-bion/krossbow/issues/151)

**Fixed bugs:**

- Ktor adapter tries to consume incomingFrames channel twice [\#155](https://github.com/joffrey-bion/krossbow/issues/155)

## [2.5.0](https://github.com/joffrey-bion/krossbow/tree/2.5.0) (2021-09-13)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.4.1...2.5.0)

**Upgraded dependencies:**

- Upgrade Jackson to version 2.12.5 [\#147](https://github.com/joffrey-bion/krossbow/issues/147)
- Upgrade uuid to version 0.3.1 [\#146](https://github.com/joffrey-bion/krossbow/issues/146)
- Upgrade Ktor client to version 1.6.3 [\#145](https://github.com/joffrey-bion/krossbow/issues/145)
- Upgrade Kotlinx Coroutines to version 1.5.2 [\#144](https://github.com/joffrey-bion/krossbow/issues/144)
- Upgrade Kotlin to version 1.5.30 [\#136](https://github.com/joffrey-bion/krossbow/issues/136)

## [2.4.1](https://github.com/joffrey-bion/krossbow/tree/2.4.1) (2021-09-08)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.4.0...2.4.1)

**Implemented enhancements:**

- Ensure all web socket clients throw `WebSocketConnectionException` when failing to connect [\#137](https://github.com/joffrey-bion/krossbow/issues/137)

**Upgraded dependencies:**

- Upgrade OkHttp to version 4.9.1 [\#139](https://github.com/joffrey-bion/krossbow/issues/139)

**Fixed bugs:**

- OkHttp's close frame is not always correctly reported [\#140](https://github.com/joffrey-bion/krossbow/issues/140)

## [2.4.0](https://github.com/joffrey-bion/krossbow/tree/2.4.0) (2021-09-05)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.3.0...2.4.0)

**Implemented enhancements:**

- Add adapter for Spring Jetty's implementation [\#134](https://github.com/joffrey-bion/krossbow/issues/134)

**Deprecations:**

- Deprecate global defaultWebSocketClient\(\) in favor of WebSocketClient.default\(\) [\#132](https://github.com/joffrey-bion/krossbow/issues/132)

**Upgraded dependencies:**

- Upgrade spring\-websocket to version 5.3.9 [\#133](https://github.com/joffrey-bion/krossbow/issues/133)
- Upgrade Kotlin Coroutines to version 1.5.1 [\#131](https://github.com/joffrey-bion/krossbow/issues/131)

## [2.3.0](https://github.com/joffrey-bion/krossbow/tree/2.3.0) (2021-08-14)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.2.1...2.3.0)

**Implemented enhancements:**

- Publish common modules with iOS target \(without built\-in iOS client\) [\#130](https://github.com/joffrey-bion/krossbow/issues/130)

## [2.2.1](https://github.com/joffrey-bion/krossbow/tree/2.2.1) (2021-08-12)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.2.0...2.2.1)

**Fixed bugs:**

- OkHttp throws IAE "reason.size\(\) \> 123" when another exception occurs [\#129](https://github.com/joffrey-bion/krossbow/issues/129)

## [2.2.0](https://github.com/joffrey-bion/krossbow/tree/2.2.0) (2021-07-27)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.1.0...2.2.0)

**Upgraded dependencies:**

- Upgrade Dokka to version 1.5.0 [\#127](https://github.com/joffrey-bion/krossbow/issues/127)
- Upgrade Kotlin to version 1.5.21 [\#124](https://github.com/joffrey-bion/krossbow/issues/124)

**Fixed bugs:**

- Send frame should suspend until delivery of the message  [\#125](https://github.com/joffrey-bion/krossbow/issues/125)

## [2.1.0](https://github.com/joffrey-bion/krossbow/tree/2.1.0) (2021-06-16)
[View commits](https://github.com/joffrey-bion/krossbow/compare/2.0.0...2.1.0)

**Implemented enhancements:**

- Make `host` header more easily customizable [\#123](https://github.com/joffrey-bion/krossbow/issues/123)
- Allow sending CONNECT frame without `host` header [\#122](https://github.com/joffrey-bion/krossbow/issues/122)

## [2.0.0](https://github.com/joffrey-bion/krossbow/tree/2.0.0) (2021-05-28)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.4.0...2.0.0)

**Breaking changes:**

- Align StompSession.use with Closeable.use [\#106](https://github.com/joffrey-bion/krossbow/issues/106)

**Upgraded dependencies:**

- Upgrade Ktor to version 1.6.0 \(Kotlin 1.5\) [\#119](https://github.com/joffrey-bion/krossbow/issues/119)
- Upgrade Kotlin coroutines to version 1.5.0 [\#117](https://github.com/joffrey-bion/krossbow/issues/117)
- Upgrade Kotlinx Serialization to version 1.2.0 [\#115](https://github.com/joffrey-bion/krossbow/issues/115)
- Upgrade Kotlin to version 1.5 [\#109](https://github.com/joffrey-bion/krossbow/issues/109)

**Fixed bugs:**

- withTransaction doesn't allow suspending calls inside [\#114](https://github.com/joffrey-bion/krossbow/issues/114)

## [1.4.0](https://github.com/joffrey-bion/krossbow/tree/1.4.0) (2021-05-11)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.3.1...1.4.0)

**Upgraded dependencies:**

- Upgrade Dokka to version 1.4.32 [\#113](https://github.com/joffrey-bion/krossbow/issues/113)
- Upgrade Jackson to version 2.12.3 [\#112](https://github.com/joffrey-bion/krossbow/issues/112)
- Upgrade spring\-websocket to version 5.3.6 [\#111](https://github.com/joffrey-bion/krossbow/issues/111)
- Upgrade Ktor to version 1.5.4 [\#110](https://github.com/joffrey-bion/krossbow/issues/110)
- Upgrade Atomicfu to version 0.15.0 [\#108](https://github.com/joffrey-bion/krossbow/issues/108)

## [1.3.1](https://github.com/joffrey-bion/krossbow/tree/1.3.1) (2021-04-21)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.3.0...1.3.1)

**Fixed bugs:**

- subscribe\(\) does not respect custom headers when using generated ID [\#107](https://github.com/joffrey-bion/krossbow/issues/107)

## [1.3.0](https://github.com/joffrey-bion/krossbow/tree/1.3.0) (2021-04-20)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.2.0...1.3.0)

**Implemented enhancements:**

- Feature \- WebSocket Reconnecting Strategy [\#94](https://github.com/joffrey-bion/krossbow/issues/94)
- Migrate from kotlinx\-io to okio [\#97](https://github.com/joffrey-bion/krossbow/issues/97)

**Deprecations:**

- Rename WebSocketSession to WebSocketConnection [\#100](https://github.com/joffrey-bion/krossbow/issues/100)

**Closed issues:**

- Remove dependency on kotlinx\-io's charsets [\#99](https://github.com/joffrey-bion/krossbow/issues/99)

**Merged pull requests:**

- Add custom header support to SUBSCRIBE frame. [\#105](https://github.com/joffrey-bion/krossbow/pull/105) ([@jatsqi](https://github.com/jatsqi))

**Upgraded dependencies:**

- Upgrade Kotlin coroutines to version 1.4.3 [\#101](https://github.com/joffrey-bion/krossbow/issues/101)

## [1.2.0](https://github.com/joffrey-bion/krossbow/tree/1.2.0) (2021-03-20)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.1.5...1.2.0)

**Upgraded dependencies:**

- Upgrade Kotlinx Serialization to version 1.1.0 [\#96](https://github.com/joffrey-bion/krossbow/issues/96)
- Upgrade Kotlin to version 1.4.31 [\#95](https://github.com/joffrey-bion/krossbow/issues/95)

## [1.1.5](https://github.com/joffrey-bion/krossbow/tree/1.1.5) (2021-01-19)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.1.4...1.1.5)

**Implemented enhancements:**

- Improve performance of Spring and JDK11 websocket with mutex instead of thread [\#92](https://github.com/joffrey-bion/krossbow/issues/92)

## [1.1.4](https://github.com/joffrey-bion/krossbow/tree/1.1.4) (2021-01-17)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.1.3...1.1.4)

**Fixed bugs:**

- Make JDK11 WebSocket thread safe [\#91](https://github.com/joffrey-bion/krossbow/issues/91)

## [1.1.3](https://github.com/joffrey-bion/krossbow/tree/1.1.3) (2021-01-08)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.1.2...1.1.3)

**Implemented enhancements:**

- Stop transferring experimental status of Kotlinx Serialization [\#87](https://github.com/joffrey-bion/krossbow/issues/87)

**Upgraded dependencies:**

- Upgrade Kotlin to version 1.4.20 [\#89](https://github.com/joffrey-bion/krossbow/issues/89)

## [1.1.2](https://github.com/joffrey-bion/krossbow/tree/1.1.2) (2020-12-05)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.1.1...1.1.2)

**Fixed bugs:**

- Invalid STOMP frame error for UTF\-8 body [\#86](https://github.com/joffrey-bion/krossbow/issues/86)

## [1.1.1](https://github.com/joffrey-bion/krossbow/tree/1.1.1) (2020-11-25)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.1.0...1.1.1)

**Implemented enhancements:**

- Support PING/PONG web socket frames for engines that support it [\#84](https://github.com/joffrey-bion/krossbow/issues/84)
- Add configurable web socket frame buffer [\#83](https://github.com/joffrey-bion/krossbow/issues/83)

**Upgraded dependencies:**

- Upgrade Kotlinx Serialization to version 1.0.1 [\#85](https://github.com/joffrey-bion/krossbow/issues/85)

## [1.1.0](https://github.com/joffrey-bion/krossbow/tree/1.1.0) (2020-11-13)
[View commits](https://github.com/joffrey-bion/krossbow/compare/1.0.0...1.1.0)

**Implemented enhancements:**

- Add WebSocketSession.stomp\(\) extension [\#81](https://github.com/joffrey-bion/krossbow/issues/81)

**Upgraded dependencies:**

- Upgrade Kotlin coroutines to version 1.4.1 [\#82](https://github.com/joffrey-bion/krossbow/issues/82)

## [1.0.0](https://github.com/joffrey-bion/krossbow/tree/1.0.0) (2020-10-31)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.43.0...1.0.0)

**Upgraded dependencies:**

- Upgrade spring\-websocket to version 5.3.0 [\#80](https://github.com/joffrey-bion/krossbow/issues/80)
- Upgrade SockJS client to version 1.5.0 [\#79](https://github.com/joffrey-bion/krossbow/issues/79)
- Upgrade Ktor to version 1.4.1 [\#78](https://github.com/joffrey-bion/krossbow/issues/78)
- Upgrade jackson to version 2.11.3 [\#77](https://github.com/joffrey-bion/krossbow/issues/77)
- Upgrade OkHttp to version 4.9.0 [\#76](https://github.com/joffrey-bion/krossbow/issues/76)
- Upgrade Kotlin coroutines to version 1.4.0 [\#75](https://github.com/joffrey-bion/krossbow/issues/75)

## [0.43.0](https://github.com/joffrey-bion/krossbow/tree/0.43.0) (2020-10-10)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.42.0...0.43.0)

**Upgraded dependencies:**

- Upgrade Kotlin to version 1.4.10 [\#74](https://github.com/joffrey-bion/krossbow/issues/74)
- Upgrade kotlinx.serialization to version 1.0 [\#73](https://github.com/joffrey-bion/krossbow/issues/73)

**Fixed bugs:**

- Spring websocket adapter deadlocks on close\(\) [\#72](https://github.com/joffrey-bion/krossbow/issues/72)
- JDK11 websocket adapter doesn't let the received CLOSE frame through [\#71](https://github.com/joffrey-bion/krossbow/issues/71)

## [0.42.0](https://github.com/joffrey-bion/krossbow/tree/0.42.0) (2020-09-23)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.41.0...0.42.0)

**Upgraded dependencies:**

- Upgrade Kotlinx Serialization to version 1.0.0\-RC2 [\#70](https://github.com/joffrey-bion/krossbow/issues/70)

## [0.41.0](https://github.com/joffrey-bion/krossbow/tree/0.41.0) (2020-09-13)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.40.0...0.41.0)

**Implemented enhancements:**

- Add adapter for Ktor's websocket implementation [\#69](https://github.com/joffrey-bion/krossbow/issues/69)

## [0.40.0](https://github.com/joffrey-bion/krossbow/tree/0.40.0) (2020-09-12)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.32.0...0.40.0)

**Breaking changes:**

- Make subscribe methods suspending [\#68](https://github.com/joffrey-bion/krossbow/issues/68)

## [0.32.0](https://github.com/joffrey-bion/krossbow/tree/0.32.0) (2020-09-07)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.31.0...0.32.0)

**Implemented enhancements:**

- Add instrumentation option to monitor/log/debug internal events [\#67](https://github.com/joffrey-bion/krossbow/issues/67)

## [0.31.0](https://github.com/joffrey-bion/krossbow/tree/0.31.0) (2020-09-04)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.30.1...0.31.0)

**Implemented enhancements:**

- Add custom headers support to the STOMP CONNECT frame [\#64](https://github.com/joffrey-bion/krossbow/issues/64)

**Merged pull requests:**

- Add custom headers support to CONNECT frame [\#65](https://github.com/joffrey-bion/krossbow/pull/65) ([@ghost](https://github.com/ghost))

## [0.30.1](https://github.com/joffrey-bion/krossbow/tree/0.30.1) (2020-08-31)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.30.0...0.30.1)

**Fixed bugs:**

- Gradle metadata not published correctly [\#63](https://github.com/joffrey-bion/krossbow/issues/63)

## [0.30.0](https://github.com/joffrey-bion/krossbow/tree/0.30.0) (2020-08-30)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.21.1...0.30.0)

**Closed issues:**

- Migrate to Kotlin 1.4 & Serialization 1.0.0\-RC [\#62](https://github.com/joffrey-bion/krossbow/issues/62)

## [0.21.1](https://github.com/joffrey-bion/krossbow/tree/0.21.1) (2020-06-02)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.21.0...0.21.1)

**Fixed bugs:**

- Incorrect heart beats periods [\#60](https://github.com/joffrey-bion/krossbow/issues/60)

## [0.21.0](https://github.com/joffrey-bion/krossbow/tree/0.21.0) (2020-06-02)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.20.3...0.21.0)

**Implemented enhancements:**

- Make the heartbeats margins configurable [\#59](https://github.com/joffrey-bion/krossbow/issues/59)

**Fixed bugs:**

- Jdk11WebSocketClient wraps cancellations in WebSocketConnectionException [\#58](https://github.com/joffrey-bion/krossbow/issues/58)

## [0.20.3](https://github.com/joffrey-bion/krossbow/tree/0.20.3) (2020-05-15)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.20.2...0.20.3)

**Fixed bugs:**

- Web socket close\(\) attempted on closed socket [\#57](https://github.com/joffrey-bion/krossbow/issues/57)

## [0.20.2](https://github.com/joffrey-bion/krossbow/tree/0.20.2) (2020-05-15)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.20.1...0.20.2)

**Fixed bugs:**

- \[JS\] DOMException: Failed to execute 'close' on 'WebSocket' [\#56](https://github.com/joffrey-bion/krossbow/issues/56)
- Spring web socket adapter is not thread safe [\#55](https://github.com/joffrey-bion/krossbow/issues/55)

## [0.20.1](https://github.com/joffrey-bion/krossbow/tree/0.20.1) (2020-05-10)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.20.0...0.20.1)

**Fixed bugs:**

- Should not send UNSUBSCRIBE frames after DISCONNECT [\#54](https://github.com/joffrey-bion/krossbow/issues/54)

## [0.20.0](https://github.com/joffrey-bion/krossbow/tree/0.20.0) (2020-05-04)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.12.0...0.20.0)

**Breaking changes:**

- Represent subscriptions with Kotlin Flows [\#53](https://github.com/joffrey-bion/krossbow/issues/53)

**Implemented enhancements:**

- Add base adapters to support all Kotlinx Serialization formats [\#52](https://github.com/joffrey-bion/krossbow/issues/52)

**Fixed bugs:**

- Textual content\-type on binary websocket frames is always decoded as UTF\-8 [\#51](https://github.com/joffrey-bion/krossbow/issues/51)

## [0.12.0](https://github.com/joffrey-bion/krossbow/tree/0.12.0) (2020-04-24)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.11.1...0.12.0)

**Implemented enhancements:**

- Support for STOMP frame instead of CONNECT [\#50](https://github.com/joffrey-bion/krossbow/issues/50)
- Add support for transaction frames \(BEGIN/COMMIT/ABORT\) [\#45](https://github.com/joffrey-bion/krossbow/issues/45)
- Add support for ACK/NACK frames [\#44](https://github.com/joffrey-bion/krossbow/issues/44)

**Fixed bugs:**

- LostReceiptException thrown on external timeout [\#48](https://github.com/joffrey-bion/krossbow/issues/48)

## [0.11.1](https://github.com/joffrey-bion/krossbow/tree/0.11.1) (2020-04-15)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.11.0...0.11.1)

**Fixed bugs:**

- Heart beats create deadlock [\#46](https://github.com/joffrey-bion/krossbow/issues/46)
- Receiving EOL \(heart beat\) crashes STOMP decoder [\#43](https://github.com/joffrey-bion/krossbow/issues/43)
- Race condition issues with Jdk11 partial frames [\#42](https://github.com/joffrey-bion/krossbow/issues/42)

## [0.11.0](https://github.com/joffrey-bion/krossbow/tree/0.11.0) (2020-04-12)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.10.3...0.11.0)

**Implemented enhancements:**

- Rename krossbow\-websocket\-api to krossbow\-websocket\-core [\#41](https://github.com/joffrey-bion/krossbow/issues/41)
- Add OkHttp websocket adapter [\#40](https://github.com/joffrey-bion/krossbow/issues/40)
- Add support for websocket frame splitting in common code [\#36](https://github.com/joffrey-bion/krossbow/issues/36)

## [0.10.3](https://github.com/joffrey-bion/krossbow/tree/0.10.3) (2020-03-27)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.10.2...0.10.3)

**Upgraded dependencies:**

- Upgrade Kotlinx.serialization to version 0.20.0 [\#39](https://github.com/joffrey-bion/krossbow/issues/39)
- Upgrade Jackson to version 2.10.0 [\#38](https://github.com/joffrey-bion/krossbow/issues/38)

## [0.10.2](https://github.com/joffrey-bion/krossbow/tree/0.10.2) (2020-03-19)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.10.1...0.10.2)

**Breaking changes:**

- Change default WebSocket impl on JVM to JDK 11 async client [\#34](https://github.com/joffrey-bion/krossbow/issues/34)
- Rework WebSocket API to use a channel instead of a listener [\#32](https://github.com/joffrey-bion/krossbow/issues/32)

**Implemented enhancements:**

- Add support for heart beats [\#33](https://github.com/joffrey-bion/krossbow/issues/33)
- Add JDK11 async WebSocket bridge [\#27](https://github.com/joffrey-bion/krossbow/issues/27)

## [0.10.1](https://github.com/joffrey-bion/krossbow/tree/0.10.1) (2020-03-12)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.10.0...0.10.1)

**Implemented enhancements:**

- Extract Jackson and Kotlinx Serialization as separate artifacts [\#28](https://github.com/joffrey-bion/krossbow/issues/28)
- Improve error handling [\#26](https://github.com/joffrey-bion/krossbow/issues/26)

**Fixed bugs:**

- ClassCastException in Javascript websocket onclose [\#30](https://github.com/joffrey-bion/krossbow/issues/30)
- JavaScript WebSocket adapter doesn't fail the connection with onclose [\#29](https://github.com/joffrey-bion/krossbow/issues/29)

## [0.10.0](https://github.com/joffrey-bion/krossbow/tree/0.10.0) (2020-03-09)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.4.2...0.10.0)

**Implemented enhancements:**

- Allow custom WS client/transports in SpringKrossbowEngine [\#21](https://github.com/joffrey-bion/krossbow/issues/21)
- Add support for custom headers [\#16](https://github.com/joffrey-bion/krossbow/issues/16)
- Implement pure Kotlin STOMP protocol in common code [\#5](https://github.com/joffrey-bion/krossbow/issues/5)

## [0.4.2](https://github.com/joffrey-bion/krossbow/tree/0.4.2) (2020-01-23)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.4.1...0.4.2)

**Implemented enhancements:**

- Expose Kotlinx.Serialization dependency [\#25](https://github.com/joffrey-bion/krossbow/issues/25)

## [0.4.1](https://github.com/joffrey-bion/krossbow/tree/0.4.1) (2019-12-31)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.4.0...0.4.1)

**Fixed bugs:**

- Inline subscribe\(\) method not properly declared [\#22](https://github.com/joffrey-bion/krossbow/issues/22)
- Ambiguous send\(\) overload for no\-payload calls [\#23](https://github.com/joffrey-bion/krossbow/issues/23)

## [0.4.0](https://github.com/joffrey-bion/krossbow/tree/0.4.0) (2019-12-04)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.3.2...0.4.0)

**Implemented enhancements:**

- Allow subscriptions for arbitrary/bytes/string payload [\#15](https://github.com/joffrey-bion/krossbow/issues/15)

**Fixed bugs:**

- JS version not working [\#20](https://github.com/joffrey-bion/krossbow/issues/20)

## [0.3.2](https://github.com/joffrey-bion/krossbow/tree/0.3.2) (2019-07-12)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.3.1...0.3.2)

**Implemented enhancements:**

- Allow null payloads for Unit type subscriptions [\#18](https://github.com/joffrey-bion/krossbow/issues/18)

**Fixed bugs:**

- Artifacts not uploaded to Maven Central [\#19](https://github.com/joffrey-bion/krossbow/issues/19)

## [0.3.1](https://github.com/joffrey-bion/krossbow/tree/0.3.1) (2019-07-07)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.3.0...0.3.1)

**Fixed bugs:**

- Absent payloads API is not public [\#17](https://github.com/joffrey-bion/krossbow/issues/17)

## [0.3.0](https://github.com/joffrey-bion/krossbow/tree/0.3.0) (2019-07-06)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.2.1...0.3.0)

**Implemented enhancements:**

- Allow subscriptions for empty payloads [\#14](https://github.com/joffrey-bion/krossbow/issues/14)

## [0.2.1](https://github.com/joffrey-bion/krossbow/tree/0.2.1) (2019-07-06)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.2.0...0.2.1)

**Implemented enhancements:**

- Publish artifacts to Maven Central [\#12](https://github.com/joffrey-bion/krossbow/issues/12)
- Allow null payloads in send\(\) [\#8](https://github.com/joffrey-bion/krossbow/issues/8)

**Fixed bugs:**

- autoReceipt and receiptTimeLimit are not editable [\#13](https://github.com/joffrey-bion/krossbow/issues/13)

## [0.2.0](https://github.com/joffrey-bion/krossbow/tree/0.2.0) (2019-07-05)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.1.2...0.2.0)

**Implemented enhancements:**

- Make autoReceipt and receipt timeout configurable [\#11](https://github.com/joffrey-bion/krossbow/issues/11)
- Make connect\(\) function actually non\-blocking by avoiding get\(\) [\#9](https://github.com/joffrey-bion/krossbow/issues/9)
- Make send\(\) function actually suspend until RECEIPT is received [\#7](https://github.com/joffrey-bion/krossbow/issues/7)

## [0.1.2](https://github.com/joffrey-bion/krossbow/tree/0.1.2) (2019-07-01)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.1.1...0.1.2)

**Implemented enhancements:**

- Publish krossbow\-engine\-webstompjs artifact on Jcenter [\#10](https://github.com/joffrey-bion/krossbow/issues/10)

## [0.1.1](https://github.com/joffrey-bion/krossbow/tree/0.1.1) (2019-06-25)
[View commits](https://github.com/joffrey-bion/krossbow/compare/0.1.0...0.1.1)

**Closed issues:**

- Publish Maven artifacts on Jcenter [\#6](https://github.com/joffrey-bion/krossbow/issues/6)

## [0.1.0](https://github.com/joffrey-bion/krossbow/tree/0.1.0) (2019-06-23)
[View commits](https://github.com/joffrey-bion/krossbow/compare/d274bafb8ba391187c7b64378b5e4071a869581a...0.1.0)

**Implemented enhancements:**

- Add basic connect/send/subscribe features for JS [\#2](https://github.com/joffrey-bion/krossbow/issues/2)
- Add basic connect/send/subscribe features for JVM [\#1](https://github.com/joffrey-bion/krossbow/issues/1)
