# Change Log

## [0.41.0](https://github.com/joffrey-bion/krossbow/tree/0.41.0) (2020-09-13)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.40.0...0.41.0)

**Implemented enhancements:**

- Add adapter for Ktor's websocket implementation [\#69](https://github.com/joffrey-bion/krossbow/issues/69)

## [0.40.0](https://github.com/joffrey-bion/krossbow/tree/0.40.0) (2020-09-13)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.32.0...0.40.0)

**Implemented enhancements:**

- Make subscribe methods suspending [\#68](https://github.com/joffrey-bion/krossbow/issues/68)

## [0.32.0](https://github.com/joffrey-bion/krossbow/tree/0.32.0) (2020-09-07)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.31.0...0.32.0)

**Implemented enhancements:**

- Add instrumentation option to monitor/log/debug internal events [\#67](https://github.com/joffrey-bion/krossbow/issues/67)

## [0.31.0](https://github.com/joffrey-bion/krossbow/tree/0.31.0) (2020-09-04)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.30.1...0.31.0)

**Implemented enhancements:**

- Add custom headers support to the STOMP CONNECT frame [\#64](https://github.com/joffrey-bion/krossbow/issues/64)

**Merged pull requests:**

- Add custom headers support to CONNECT frame [\#65](https://github.com/joffrey-bion/krossbow/pull/65) ([@Mostrapotski](https://github.com/Mostrapotski))

## [0.30.1](https://github.com/joffrey-bion/krossbow/tree/0.30.1) (2020-08-31)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.30.0...0.30.1)

**Fixed bugs:**

- Gradle metadata not published correctly [\#63](https://github.com/joffrey-bion/krossbow/issues/63)

## [0.30.0](https://github.com/joffrey-bion/krossbow/tree/0.30.0) (2020-08-30)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.21.1...0.30.0)

**Closed issues:**

- Migrate to Kotlin 1.4 & Serialization 1.0.0\-RC [\#62](https://github.com/joffrey-bion/krossbow/issues/62)

## [0.21.1](https://github.com/joffrey-bion/krossbow/tree/0.21.1) (2020-06-02)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.21.0...0.21.1)

**Fixed bugs:**

- Incorrect heart beats periods [\#60](https://github.com/joffrey-bion/krossbow/issues/60)

## [0.21.0](https://github.com/joffrey-bion/krossbow/tree/0.21.0) (2020-06-02)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.20.3...0.21.0)

**Fixed bugs:**

- Jdk11WebSocketClient wraps cancellations in WebSocketConnectionException [\#58](https://github.com/joffrey-bion/krossbow/issues/58)

**Implemented enhancements:**

- Make the heartbeats margins configurable [\#59](https://github.com/joffrey-bion/krossbow/issues/59)

## [0.20.3](https://github.com/joffrey-bion/krossbow/tree/0.20.3) (2020-05-15)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.20.2...0.20.3)

**Fixed bugs:**

- Web socket close\(\) attempted on closed socket [\#57](https://github.com/joffrey-bion/krossbow/issues/57)

## [0.20.2](https://github.com/joffrey-bion/krossbow/tree/0.20.2) (2020-05-15)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.20.1...0.20.2)

**Fixed bugs:**

- \[JS\] DOMException: Failed to execute 'close' on 'WebSocket' [\#56](https://github.com/joffrey-bion/krossbow/issues/56)
- Spring web socket adapter is not thread safe [\#55](https://github.com/joffrey-bion/krossbow/issues/55)

## [0.20.1](https://github.com/joffrey-bion/krossbow/tree/0.20.1) (2020-05-10)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.20.0...0.20.1)

**Fixed bugs:**

- Should not send UNSUBSCRIBE frames after DISCONNECT [\#54](https://github.com/joffrey-bion/krossbow/issues/54)

## [0.20.0](https://github.com/joffrey-bion/krossbow/tree/0.20.0) (2020-05-05)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.12.0...0.20.0)

**Fixed bugs:**

- Textual content\-type on binary websocket frames is always decoded as UTF\-8 [\#51](https://github.com/joffrey-bion/krossbow/issues/51)

**Implemented enhancements:**

- Represent subscriptions with Kotlin Flows [\#53](https://github.com/joffrey-bion/krossbow/issues/53)
- Add base adapters to support all Kotlinx Serialization formats [\#52](https://github.com/joffrey-bion/krossbow/issues/52)

## [0.12.0](https://github.com/joffrey-bion/krossbow/tree/0.12.0) (2020-04-24)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.11.1...0.12.0)

**Fixed bugs:**

- LostReceiptException thrown on external timeout [\#48](https://github.com/joffrey-bion/krossbow/issues/48)

**Implemented enhancements:**

- Support for STOMP frame instead of CONNECT [\#50](https://github.com/joffrey-bion/krossbow/issues/50)
- Add support for transaction frames \(BEGIN/COMMIT/ABORT\) [\#45](https://github.com/joffrey-bion/krossbow/issues/45)
- Add support for ACK/NACK frames [\#44](https://github.com/joffrey-bion/krossbow/issues/44)

## [0.11.1](https://github.com/joffrey-bion/krossbow/tree/0.11.1) (2020-04-15)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.11.0...0.11.1)

**Fixed bugs:**

- Heart beats create deadlock [\#46](https://github.com/joffrey-bion/krossbow/issues/46)
- Receiving EOL \(heart beat\) crashes STOMP decoder [\#43](https://github.com/joffrey-bion/krossbow/issues/43)
- Race condition issues with Jdk11 partial frames [\#42](https://github.com/joffrey-bion/krossbow/issues/42)

## [0.11.0](https://github.com/joffrey-bion/krossbow/tree/0.11.0) (2020-04-12)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.10.3...0.11.0)

**Implemented enhancements:**

- Rename krossbow\-websocket\-api to krossbow\-websocket\-core [\#41](https://github.com/joffrey-bion/krossbow/issues/41)
- Add OkHttp websocket adapter [\#40](https://github.com/joffrey-bion/krossbow/issues/40)
- Add support for websocket frame splitting in common code [\#36](https://github.com/joffrey-bion/krossbow/issues/36)

## [0.10.3](https://github.com/joffrey-bion/krossbow/tree/0.10.3) (2020-03-27)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.10.2...0.10.3)

**Implemented enhancements:**

- Upgrade Kotlinx.serialization to 0.20.0 [\#39](https://github.com/joffrey-bion/krossbow/issues/39)
- Upgrade Jackson to 2.10.0 [\#38](https://github.com/joffrey-bion/krossbow/issues/38)

## [0.10.2](https://github.com/joffrey-bion/krossbow/tree/0.10.2) (2020-03-19)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.10.1...0.10.2)

**Implemented enhancements:**

- Change default WebSocket impl on JVM to JDK 11 async client [\#34](https://github.com/joffrey-bion/krossbow/issues/34)
- Add support for heart beats [\#33](https://github.com/joffrey-bion/krossbow/issues/33)
- Rework WebSocket API to use a channel instead of a listener [\#32](https://github.com/joffrey-bion/krossbow/issues/32)
- Add JDK11 async WebSocket bridge [\#27](https://github.com/joffrey-bion/krossbow/issues/27)

## [0.10.1](https://github.com/joffrey-bion/krossbow/tree/0.10.1) (2020-03-12)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.10.0...0.10.1)

**Fixed bugs:**

- ClassCastException in Javascript websocket onclose [\#30](https://github.com/joffrey-bion/krossbow/issues/30)
- JavaScript WebSocket adapter doesn't fail the connection with onclose [\#29](https://github.com/joffrey-bion/krossbow/issues/29)

**Implemented enhancements:**

- Extract Jackson and Kotlinx Serialization as separate artifacts [\#28](https://github.com/joffrey-bion/krossbow/issues/28)
- Improve error handling [\#26](https://github.com/joffrey-bion/krossbow/issues/26)

## [0.10.0](https://github.com/joffrey-bion/krossbow/tree/0.10.0) (2020-03-09)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.4.2...0.10.0)

**Implemented enhancements:**

- Allow custom WS client/transports in SpringKrossbowEngine [\#21](https://github.com/joffrey-bion/krossbow/issues/21)
- Add support for custom headers [\#16](https://github.com/joffrey-bion/krossbow/issues/16)
- Implement pure Kotlin STOMP protocol in common code [\#5](https://github.com/joffrey-bion/krossbow/issues/5)

## [0.4.2](https://github.com/joffrey-bion/krossbow/tree/0.4.2) (2020-01-23)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.4.1...0.4.2)

**Implemented enhancements:**

- Expose Kotlinx.Serialization dependency [\#25](https://github.com/joffrey-bion/krossbow/issues/25)

## [0.4.1](https://github.com/joffrey-bion/krossbow/tree/0.4.1) (2019-12-31)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.4.0...0.4.1)

**Fixed bugs:**

- Inline subscribe\(\) method not properly declared [\#22](https://github.com/joffrey-bion/krossbow/issues/22)
- Ambiguous send\(\) overload for no\-payload calls [\#23](https://github.com/joffrey-bion/krossbow/issues/23)

## [0.4.0](https://github.com/joffrey-bion/krossbow/tree/0.4.0) (2019-12-04)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.3.2...0.4.0)

**Fixed bugs:**

- JS version not working [\#20](https://github.com/joffrey-bion/krossbow/issues/20)

**Implemented enhancements:**

- Allow subscriptions for arbitrary/bytes/string payload [\#15](https://github.com/joffrey-bion/krossbow/issues/15)

## [0.3.2](https://github.com/joffrey-bion/krossbow/tree/0.3.2) (2019-07-12)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.3.1...0.3.2)

**Fixed bugs:**

- Artifacts not uploaded to Maven Central [\#19](https://github.com/joffrey-bion/krossbow/issues/19)

**Implemented enhancements:**

- Allow null payloads for Unit type subscriptions [\#18](https://github.com/joffrey-bion/krossbow/issues/18)

## [0.3.1](https://github.com/joffrey-bion/krossbow/tree/0.3.1) (2019-07-07)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.3.0...0.3.1)

**Fixed bugs:**

- Absent payloads API is not public [\#17](https://github.com/joffrey-bion/krossbow/issues/17)

## [0.3.0](https://github.com/joffrey-bion/krossbow/tree/0.3.0) (2019-07-07)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.2.1...0.3.0)

**Implemented enhancements:**

- Allow subscriptions for empty payloads [\#14](https://github.com/joffrey-bion/krossbow/issues/14)

## [0.2.1](https://github.com/joffrey-bion/krossbow/tree/0.2.1) (2019-07-06)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.2.0...0.2.1)

**Fixed bugs:**

- autoReceipt and receiptTimeLimit are not editable [\#13](https://github.com/joffrey-bion/krossbow/issues/13)

**Implemented enhancements:**

- Publish artifacts to Maven Central [\#12](https://github.com/joffrey-bion/krossbow/issues/12)
- Allow null payloads in send\(\) [\#8](https://github.com/joffrey-bion/krossbow/issues/8)

## [0.2.0](https://github.com/joffrey-bion/krossbow/tree/0.2.0) (2019-07-06)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.1.2...0.2.0)

**Implemented enhancements:**

- Make autoReceipt and receipt timeout configurable [\#11](https://github.com/joffrey-bion/krossbow/issues/11)
- Make connect\(\) function actually non\-blocking by avoiding get\(\) [\#9](https://github.com/joffrey-bion/krossbow/issues/9)
- Make send\(\) function actually suspend until RECEIPT is received [\#7](https://github.com/joffrey-bion/krossbow/issues/7)

## [0.1.2](https://github.com/joffrey-bion/krossbow/tree/0.1.2) (2019-07-02)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.1.1...0.1.2)

**Implemented enhancements:**

- Publish krossbow\-engine\-webstompjs artifact on Jcenter [\#10](https://github.com/joffrey-bion/krossbow/issues/10)

## [0.1.1](https://github.com/joffrey-bion/krossbow/tree/0.1.1) (2019-06-25)
[Full Changelog](https://github.com/joffrey-bion/krossbow/compare/0.1.0...0.1.1)

**Closed issues:**

- Publish Maven artifacts on Jcenter [\#6](https://github.com/joffrey-bion/krossbow/issues/6)

## [0.1.0](https://github.com/joffrey-bion/krossbow/tree/0.1.0) (2019-06-23)

**Implemented enhancements:**

- Add basic connect/send/subscribe features for JS [\#2](https://github.com/joffrey-bion/krossbow/issues/2)
- Add basic connect/send/subscribe features for JVM [\#1](https://github.com/joffrey-bion/krossbow/issues/1)
