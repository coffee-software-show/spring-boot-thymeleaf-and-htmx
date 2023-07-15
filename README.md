# Spring Boot, Thymeleaf, and HTMX

progressive, server-side rendered, multi-page applications with Spring Boot and HTMX

## walkthrough 

### basic thymeleaf and Spring Boot setup 

* start.spring.io 
* add the requisite graalvm, devtools, thymeleaf, web support

### data 

* add a `Todo` and `TodoRepository`
* create some `.sql` 
* create an initializer bean that resets the default data on `ApplicationReadyEvent` and a custom `TodosResetEvent`

### html and css 

* add some CSS to make things look a little nicer 
```css
		
		:root {
            --gutter : 0.5em;
        }

        body {
            padding: var(--gutter)
        }

        .line {
            border-top: 1px solid black;
            margin-top: var(--gutter);
            padding-top: var(--gutter);
        }

        .title input[type=text] {
            width: calc(100% - var(--gutter));
        }

        .todo {
            grid-gap:  var(--gutter) ;
            display: grid;
            grid: 'id  title buttons';
            grid-template-columns: 3vw auto 20vw;
        }


        .todo .buttons {
            grid-area: buttons;
        }

        .todo .title {
            grid-area: title;
            text-align: left;

        }

        .todo .id {
            grid-area: id;
            text-align: right;
        }
```

* setup a thymeleaf page to render the todos in a list. there are no buttons yet and no use of Thymeleaf fragments yet.

```html
<div class="todos-list">
    <div th:each="todo: ${todos}" class="todo line">
        <div class="id" th:text="${todo.id}">243</div>
        <div class="title" th:text="${todo.title}"> title</div>
        <div class="buttons">

        </div>
    </div>
</div>
```
* add a controller 
```java
    
    @GetMapping
    String todos(Model model) {
        model.addAttribute("todos", this.repository.findAll());
        return "todos";
    }

```

* let's add a delete button to the buttons in the list, introducing our first use of htmx and thymeleaf:

```html
  <button hx-confirm="Are you sure?"
                    hx-target="closest .todo"
                    hx-swap="outerHTML"
                    class="btn btn-danger"
                    hx-trigger="click"
                    th:attr="hx-delete=@{/todos/{id}(id=${todo.id})}">
                Delete
            </button>
```

* we'll need a deletion controller endpoint. It's empty, so we'll return the HTML manually here.

```java
    @ResponseBody
    @DeleteMapping(path = "/{todoId}", produces = MediaType.TEXT_HTML_VALUE)
    String delete(@PathVariable Integer todoId) {
        this.repository.findById(todoId).ifPresent(this.repository::delete);
        return "";
    }

```


* let's add a button to reset all the data in case we just wanna start from the first screen. 

```html
<div class="line">

    <button hx-post="/todos/reset"
            hx-target=".todos-list">
        Reset All
    </button>
</div>
```

* in order for this to work, we need to send down the markup for just the updated rows, nothing else. let's use thymeleaf fragments. Add `th:fragment="todos"` to the `.todos-list` element tag.

```java
    @PostMapping("/reset")
    String reset(Model model) {
        this.publisher.publishEvent(new TodosResetEvent());
        model.addAttribute("todos", this.repository.findAll());
        return "todos :: todos";
    }
```

* now, let's add a form to add new records, above the reset, and below the list. there is no  `hx-swap-oob="true" `. At first we'll work this like we did the reset button, use a fragment to render only the markup for the thing that changed.

```html
<div id="todos-form" th:fragment="todos-form" class="todos-form">
    <div class="todo line ">
        <div></div>
        <div class="title">
            <input type="text" name="new-todo" id="new-todo"/>
        </div>
        <div class="buttons">
            <button
                    hx-include="#new-todo"
                    hx-post="/todos"
                    hx-target=".todos-list"
                    hx-trigger="click">
                Add
            </button>
        </div>
    </div>
</div>
```

* add the following java code, but write it in such a way that we're rendering the markup with a fragment 

```java

    @PostMapping
    String add(@RequestParam("new-todo") String newTodo, Model model) {
        this.repository.save(new Todo(null, newTodo));
        model.addAttribute("todos", this.repository.findAll());
        return  "todos :: todos"; 
    }

```

* the problem is that we also want to reset the form state, which is a different node. So we'll use out of band updates and send the DOM markup for both the form and todo list in the same response. the trouble is that spring mvc is sort of wired to return a single response by default. We're going to use a fancy library written by Wim Deblauwe, Oliver Drohtbom, and others: `implementation 'io.github.wimdeblauwe:htmx-spring-boot-thymeleaf:2.0.1'`. Now we have a new type called `HtmxResponse`. add `hx-swap-oob="true" ` to the `.todos-form` element tag. and here's the updated java code.

```java
    @PostMapping
    HtmxResponse add(@RequestParam("new-todo") String newTodo, Model model) {
        log.debug("going to add another todo : " + newTodo);
        this.repository.save(new Todo(null, newTodo));
        model.addAttribute("todos", this.repository.findAll());
        return new HtmxResponse()
                .addTemplate("todos :: todos")
                .addTemplate("todos :: todos-form");
    }
```