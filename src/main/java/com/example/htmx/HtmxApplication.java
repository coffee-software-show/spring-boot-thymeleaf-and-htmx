package com.example.htmx;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.function.RequestPredicates;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.Arrays;

import static org.springframework.web.servlet.function.RouterFunctions.route;
import static org.springframework.web.servlet.function.ServerResponse.ok;

@SpringBootApplication
public class HtmxApplication {

    public static void main(String[] args) {
        SpringApplication.run(HtmxApplication.class, args);
    }

    @Bean
    RouterFunction<ServerResponse> htmlRoutes(TodosHandler handler) {
        return route()
                .nest(RequestPredicates.path("/todos"), builder -> builder
                        .GET(handler::todos)
                        .POST( "/reset", handler::reset)
                        .POST( handler::add)
                        .DELETE("/{todoId}", handler::delete))
                .build();
    }
}

@Component
class Initializer {

    private final TodoRepository repository;

    Initializer(TodoRepository repository) {
        this.repository = repository;
    }

    @EventListener({ApplicationReadyEvent.class, TodosResetEvent.class})
    public void reset() {
        this.repository.deleteAll();
        Arrays.stream("write a new blog,record a video on HTMX,record a new podcast episode".split(","))
                .map(s -> new Todo(null, s))
                .forEach(repository::save);
    }
}

class TodosResetEvent extends ApplicationEvent {
    TodosResetEvent() {
        super(Instant.now());
    }
}

@Slf4j
@Service
class TodosHandler {

    private final TodoRepository repository;

    private final ApplicationEventPublisher publisher;

    TodosHandler(TodoRepository repository,
                    ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    ServerResponse reset(ServerRequest request) {
        publisher.publishEvent(new TodosResetEvent());
        return ok().render("todos :: todos", this.repository.findAll());
    }

    ServerResponse add(ServerRequest request) {
        var newTodo = request.param("new-todo").orElseThrow();
        log.debug("going to add another todo : " + newTodo);
        repository.save(new Todo(null, newTodo));
        return ok().render("todos", repository.findAll());
    }

    ServerResponse delete(ServerRequest request) {
        var todoId = Integer.parseInt(request.pathVariable("todoId"));
        repository.deleteById(todoId);
        return ok().build();
    }

    ServerResponse todos(ServerRequest ignoredRequest) {
        return ok().render("todos", repository.findAll());
    }
}


interface TodoRepository extends CrudRepository<Todo, Integer> {
}

record Todo(@Id Integer id, String title) {
}