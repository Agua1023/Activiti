package org.activiti.spring.boot.process;

import org.activiti.api.model.shared.model.VariableInstance;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.process.runtime.conf.ProcessRuntimeConfiguration;
import org.activiti.engine.ActivitiException;
import org.activiti.spring.process.ExtensionVariableTypes;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.text.ParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.tuple;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration
public class ProcessExtensionsTest {

    private static final String INITIAL_VARS_PROCESS = "initialVarsProcess";

    @Autowired
    private ProcessRuntime processRuntime;

    @Test
    @WithUserDetails(value = "salaboy", userDetailsServiceBeanName = "myUserDetailsService")
    public void processInstanceHasInitialVariables() {

        ProcessRuntimeConfiguration configuration = processRuntime.configuration();
        assertThat(configuration).isNotNull();

        // start a process with vars then check default and specified vars exist
        ProcessInstance initialVarsProcess = processRuntime.start(ProcessPayloadBuilder.start()
                .withProcessDefinitionKey(INITIAL_VARS_PROCESS)
                .withVariable("extraVar",
                        true)
                .withVariable("age",
                        10)
                .withBusinessKey("my business key")
                .build());

        assertThat(initialVarsProcess).isNotNull();
        assertThat(initialVarsProcess.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);

        List<VariableInstance> variableInstances = processRuntime.variables(ProcessPayloadBuilder.variables().withProcessInstance(initialVarsProcess).build());

        assertThat(variableInstances).isNotNull();
        assertThat(variableInstances).hasSize(4);

        assertThat(variableInstances).extracting("name")
                .contains("extraVar", "name", "age", "birth")
                .doesNotContain("subscribe");

        // cleanup
        processRuntime.delete(ProcessPayloadBuilder.delete(initialVarsProcess));
    }


    @Test
    @WithUserDetails(value = "salaboy", userDetailsServiceBeanName = "myUserDetailsService")
    public void processInstanceHasValidInitialVariables() throws ParseException {

        ProcessRuntimeConfiguration configuration = processRuntime.configuration();
        assertThat(configuration).isNotNull();

        // start a process with vars then check default and specified vars exist
        ProcessInstance initialVarsProcess = processRuntime.start(ProcessPayloadBuilder.start()
                .withProcessDefinitionKey(INITIAL_VARS_PROCESS)
                .withVariable("extraVar",
                        true)
                .withVariable("age",
                        10)
                .withVariable("name",
                        "bob")
                .withVariable("subscribe",
                        true)
                .withVariable("birth", ExtensionVariableTypes.DATE.getDateFormat().parse("2009-11-30"))
                .withBusinessKey("my business key")
                .build());

        assertThat(initialVarsProcess).isNotNull();
        assertThat(initialVarsProcess.getStatus()).isEqualTo(ProcessInstance.ProcessInstanceStatus.RUNNING);

        List<VariableInstance> variableInstances = processRuntime.variables(ProcessPayloadBuilder.variables().withProcessInstance(initialVarsProcess).build());

        assertThat(variableInstances).isNotNull();
        assertThat(variableInstances).hasSize(5);

        assertThat(variableInstances).extracting("name","type")
                .contains(tuple("extraVar","boolean"),
                        tuple("name","string"),
                        tuple("age","integer"),
                        tuple("birth","date"),
                        tuple("subscribe","boolean"));

        // cleanup
        processRuntime.delete(ProcessPayloadBuilder.delete(initialVarsProcess));
    }


    @Test
    @WithUserDetails(value = "salaboy", userDetailsServiceBeanName = "myUserDetailsService")
    public void processInstanceFailsWithoutRequiredVariables() {
        ProcessRuntimeConfiguration configuration = processRuntime.configuration();
        assertThat(configuration).isNotNull();

        assertThatExceptionOfType(ActivitiException.class).isThrownBy(() -> {
            processRuntime.start(ProcessPayloadBuilder.start()
                    .withProcessDefinitionKey(INITIAL_VARS_PROCESS)
                    .withVariable("extraVar",
                            true)
                    .build());
        }).withMessage("Can't start process '" + INITIAL_VARS_PROCESS + "' without required variables age");
    }

    @Test
    @WithUserDetails(value = "salaboy", userDetailsServiceBeanName = "myUserDetailsService")
    public void processInstanceFailsIfVariableTypeIncorrect() {
        ProcessRuntimeConfiguration configuration = processRuntime.configuration();
        assertThat(configuration).isNotNull();

        assertThatExceptionOfType(ActivitiException.class).isThrownBy(() -> {
            processRuntime.start(ProcessPayloadBuilder.start()
                    .withProcessDefinitionKey(INITIAL_VARS_PROCESS)
                    .withVariable("age", true)
                    .withVariable("name",7)
                    .withVariable("subscribe","ok")
                    .withVariable("birth","thisisnotadate")
                    .build());
        }).withMessage("Can't start process '" + INITIAL_VARS_PROCESS + "' as variables have unexpected types subscribe, name, birth, age");
    }
}
