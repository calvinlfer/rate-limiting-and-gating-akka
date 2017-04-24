import ch.qos.logback.core.*
import ch.qos.logback.classic.encoder.PatternLayoutEncoder

appender(name="CONSOLE", clazz=ConsoleAppender) {
    encoder(PatternLayoutEncoder) {
        pattern = "name=example date=%date{ISO8601} level=%level actor=%X{akkaSource} message=%msg\n"
    }
}

logger(name="com.github.calvin", level=DEBUG)

root(level=INFO, appenderNames=["CONSOLE"])
