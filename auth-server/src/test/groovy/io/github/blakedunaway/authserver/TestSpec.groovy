package io.github.blakedunaway.authserver

import io.github.blakedunaway.authserver.config.TestConfig
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = TestConfig)
abstract class TestSpec extends Specification {}
