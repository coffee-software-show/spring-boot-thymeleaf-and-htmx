package com.example.htmx;

import io.github.wimdeblauwe.hsbt.mvc.HtmxResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.stream.Stream;

@SpringBootApplication
public class HtmxApplication {

    public static void main(String[] args) {
        SpringApplication.run(HtmxApplication.class, args);
    }

}

@Component
class Initializer {

    private final TodoRepository repository;

    Initializer(TodoRepository repository) {
        this.repository = repository;
    }

    @EventListener({ApplicationReadyEvent.class, TodosResetEvent.class})
    void reset() {
        this.repository.deleteAll();
        Stream.of("write a new blog,record a video on HTMX,record a new podcast episode".split(","))
                .forEach(t -> this.repository.save(new Todo(null, t)));
    }
}

class TodosResetEvent extends ApplicationEvent {
    TodosResetEvent() {
        super(Instant.now());
    }
}

@Slf4j
@Controller
@RequestMapping("/todos")
class TodosController {

    private final TodoRepository repository;

    private final ApplicationEventPublisher publisher;

    TodosController(TodoRepository repository,
                    ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    @PostMapping("/reset")
    String reset(Model model) {
        this.publisher.publishEvent(new TodosResetEvent());
        model.addAttribute("todos", this.repository.findAll());
        return "todos :: todos";
    }

    @PostMapping
    HtmxResponse add(@RequestParam("new-todo") String newTodo, Model model) {
        log.debug("going to add another todo : " + newTodo);
        this.repository.save(new Todo(null, newTodo));
        model.addAttribute("todos", this.repository.findAll());
        return new HtmxResponse()
                .addTemplate("todos :: todos")
                .addTemplate("todos :: todos-form");
    }

    @ResponseBody
    @DeleteMapping(path = "/{todoId}", produces = MediaType.TEXT_HTML_VALUE)
    String delete(@PathVariable Integer todoId) {
        this.repository.findById(todoId).ifPresent(this.repository::delete);
        return "";
    }

    @GetMapping
    String todos(Model model) {
        model.addAttribute("todos", this.repository.findAll());
        return "todos";
    }
}


interface TodoRepository extends CrudRepository<Todo, Integer> {
}

record Todo(@Id Integer id, String title) {
}