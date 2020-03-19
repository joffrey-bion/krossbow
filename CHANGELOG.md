# Change Log

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
