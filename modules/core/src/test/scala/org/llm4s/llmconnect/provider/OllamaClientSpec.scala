package org.llm4s.llmconnect.provider

import org.scalatest.funsuite.AnyFunSuite
import org.llm4s.llmconnect.model._
import org.llm4s.llmconnect.config.OllamaConfig

class OllamaClientSpec extends AnyFunSuite {

  test("ollama chat request sends assistant content as a plain string") {

    val conversation = Conversation(
      messages = Seq(
        SystemMessage("You are a helpful assistant"),
        UserMessage("Say hello"),
        // This reproduces the bug
        AssistantMessage(None, Seq.empty)
      )
    )

    val config = OllamaConfig(
      model = "llama3.1",
      baseUrl = "http://localhost:11434",
      contextWindow = 4096,
      reserveCompletion = 512
    )

    val client = new OllamaClient(config)

    // Access internal method via reflection (test-only)
    val method = client.getClass.getDeclaredMethods
      .find(_.getName.contains("createRequestBody"))
      .get

    method.setAccessible(true)

    val body = method
      .invoke(
        client,
        conversation,
        CompletionOptions(),
        Boolean.box(false)
      )
      .asInstanceOf[ujson.Obj]

    val messages = body("messages").arr

    val assistantMessage =
      messages.find(_("role").str == "assistant").get

    assert(
      assistantMessage("content").isInstanceOf[ujson.Str],
      "Expected assistant message content to be a string for Ollama"
    )
    assert(assistantMessage("content").str == "", "Assistant content should default to empty string when missing")
  }
}
