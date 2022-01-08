# Contributing

You want to contribute to Ethylene? That's great! I welcome all issues and pull requests. However, your contributions should follow some simple guidelines. 

## Issues

I'm not too picky about issues — as long as they more or less follow one of the templates provided and are legible/accurate. I'm open to consider any reasonable suggestions, but keep in mind that they should be _reasonable_ — I probably won't approve ideas that:

* Are unrelated to the stated goals/purpose of Ethylene
* Are related, but massively increase the scope of the project
* Have already been done well by other people

If you're reporting a bug, provide as many details as you can. I'll look into just about anything but it _really_ helps if I have lots to go off of. I especially like it when bug reports include lots of code samples. There's no such thing as too much detail!

## Pull Requests

Pull requests are the best way to contribute to Ethylene. In particular, I'm always looking to add new modules which provide support for specific configuration formats. Any code you write for Ethylene should:

* Work properly 
* Use unit tests where appropriate (you don't need to unit test everything, but you should unit test any complex or error-prone code)
* Follow the style guidelines
* Provide proper javadoc for all _public_ or _protected_ members

Pull requests should be made to the `dev` branch, not the `main` branch. If you make a pull request, I'll review it myself and correct any minor issues or inconsistencies. If your code doesn't work and I can't easily see the fix, I'll notify you. 

If you try to pull request something questionable, I might outright deny it. If you have doubts about whether or not your contribution would be accepted, create an issue for it _first_, before writing any code. 

### Style Guidelines

Code style isn't hugely important. However, contributions should follow some basic rules:

* Follow standard, generally accepted Java style (`camelCase` for local variables and instance variables, `SHOUTY_SNAKE_CASE` for `static final` fields, `CamelCase` for classnames, [recommended package naming conventions](https://docs.oracle.com/javase/tutorial/java/package/namingpkgs.html), ...
* Use Java 16 language features where appropriate.
  * However, don't use `var`. This might be controversial, but I made the decision to not use `var` in any of the Ethylene modules I've made, and that should remain consistent.
* Include lots of comments, particularly whenever methods or classes get complicated. You don't _need_ to add comments for code that's super self-explanatory, but it doesn't hurt either.
* Use JetBrains @Nullable and @NonNull annotations on all non-private method/constructor parameters to correctly indicate if they should accept or return null. Be careful with using @Nullable, however, as it implies that it is necessary to null-check the value. Consult the documentation for more details.
* Use `Objects#requireNonNull(Object)` to null-check arguments in public constructors and methods, where reasonable. 
* In general, try to make sure your code is _fail-fast_ — validate input where it is reasonable to do so, and throw an appropriate exception if it fails validation.
* Include correct, complete, and descriptive Javadoc for all _non-private_ members. Running the command `gradlew clean javadoc` should complete successfully with no warnings or errors.
  * Javadoc should be written in proper English, without major typos or grammatical issues.
  * When mentioning other classes in Javadoc, use @link the first time it is mentioned. You don't have to do this for "common" classes like `String` or `Object`. 
  * Methods should, within reason, use @throws to describe all of the exceptions that can be thrown.
* Avoid duplicating code.
* Don't use Lombok. 
* Avoid introducing unnecessary dependencies. For example, there's no need to add a dependency on Apache Commons if you're only using a few very simple features from it.
* Avoid shading dependencies unnecessarily.
  * If you do decide to shade anyway, dependencies should be _relocated_ so that they won't interfere with consumers who may have a direct dependency on the shaded artifact. 

## Contributing directly

I'm not currently looking to add any new direct contributors, sorry.
