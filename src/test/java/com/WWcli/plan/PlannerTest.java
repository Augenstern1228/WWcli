package com.WWcli.plan;

import com.WWcli.llm.GLMClient;
import com.WWcli.llm.LlmClient;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlannerTest {

    @Test
    void createsMinimalPlanForSimpleGoalWithoutCallingLlm() throws Exception {
        Planner planner = new Planner(new FailingGLMClient());

        ExecutionPlan plan = planner.createPlan("列出当前目录的文件");

        assertEquals("直接执行简单任务：列出当前目录的文件", plan.getSummary());
        assertEquals(List.of("task_1"), plan.getExecutionOrder());
        Task task = plan.getTask("task_1");
        assertEquals(Task.TaskType.COMMAND, task.getType());
        assertEquals("列出当前目录的文件", task.getDescription());
    }

    @Test
    void delegatesComplexGoalToLlmPlannerPath() throws Exception {
        StubGLMClient client = new StubGLMClient("""
                {
                  "summary": "复杂任务",
                  "tasks": [
                    {
                      "id": "task_a",
                      "description": "先读取 pom.xml",
                      "type": "FILE_READ",
                      "dependencies": []
                    },
                    {
                      "id": "task_b",
                      "description": "再验证项目结构",
                      "type": "VERIFICATION",
                      "dependencies": ["task_a"]
                    }
                  ]
                }
                """);
        Planner planner = new Planner(client);
        planner.setProjectMemorySupplier(() -> "## WW.md 项目记忆\n- 计划前必须读取项目规则");

        ExecutionPlan plan = planner.createPlan("先读取 pom.xml 然后验证项目结构");

        assertEquals("复杂任务", plan.getSummary());
        assertEquals(2, plan.getAllTasks().size());
        assertTrue(plan.getTask("task_2").getDependencies().contains("task_1"));
        assertTrue(client.lastSystemPrompt.contains("计划前必须读取项目规则"));
    }

    @Test
    void rejectsPlanWithMissingDependencyBeforeExecution() {
        Planner planner = new Planner(new StubGLMClient("""
                {
                  "summary": "缺失依赖",
                  "tasks": [
                    {
                      "id": "task_a",
                      "description": "分析模块",
                      "type": "ANALYSIS",
                      "dependencies": []
                    },
                    {
                      "id": "task_b",
                      "description": "验证结果",
                      "type": "VERIFICATION",
                      "dependencies": ["task_missing"]
                    }
                  ]
                }
                """));

        IOException error = assertThrows(IOException.class,
                () -> planner.createPlan("先分析模块，然后验证完整结果"));

        assertTrue(error.getMessage().contains("task_b"));
        assertTrue(error.getMessage().contains("task_missing"));
    }

    @Test
    void rejectsCyclicPlanWithConcreteTaskPath() {
        Planner planner = new Planner(new StubGLMClient("""
                {
                  "summary": "循环依赖",
                  "tasks": [
                    {
                      "id": "task_a",
                      "description": "分析模块 A",
                      "type": "ANALYSIS",
                      "dependencies": ["task_b"]
                    },
                    {
                      "id": "task_b",
                      "description": "分析模块 B",
                      "type": "ANALYSIS",
                      "dependencies": ["task_a"]
                    }
                  ]
                }
                """));

        IOException error = assertThrows(IOException.class,
                () -> planner.createPlan("先分析模块 A，然后分析模块 B 并汇总"));

        assertTrue(error.getMessage().contains("循环依赖"));
        assertTrue(error.getMessage().contains("task_1 -> task_2 -> task_1"));
    }

    private static final class FailingGLMClient extends GLMClient {
        private FailingGLMClient() {
            super("test-key");
        }

        @Override
        public ChatResponse chat(List<Message> messages, List<Tool> tools, StreamListener listener) throws IOException {
            throw new IOException("simple goal should not call llm");
        }
    }

    private static final class StubGLMClient extends GLMClient {
        private final String content;
        private String lastSystemPrompt = "";

        private StubGLMClient(String content) {
            super("test-key");
            this.content = content;
        }

        @Override
        public ChatResponse chat(List<Message> messages, List<Tool> tools, StreamListener listener) {
            this.lastSystemPrompt = messages.get(0).content();
            return new ChatResponse("assistant", content, null, 100, 20);
        }
    }
}
