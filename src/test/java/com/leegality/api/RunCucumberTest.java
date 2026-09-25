package com.leegality.api;

import io.cucumber.junit.Cucumber;
import io.cucumber.junit.CucumberOptions;
import org.junit.runner.RunWith;

@RunWith(Cucumber.class)
@CucumberOptions(features="src/test/resources/features", glue="com.leegality.api.steps", plugin={"pretty", "html:target/cucumber-report.html", "json:target/cucumber.json"}, monochrome=true, publish=false)
public class RunCucumberTest {}
