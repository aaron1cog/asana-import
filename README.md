# asana-import

This project uses a Spring Boot application to import different csv data into Asana.

This project uses the [Asana Java client](https://github.com/Asana/java-asana) to interact with Asana.

This project also uses [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) for parsing the csv files.

## To Start

1. Fork the repo.
1. Set up the Asana properties to successfully connect to Asana.
    1. `asana.token` - This property needs to contain the [Asana personal access token](https://developers.asana.com/docs/personal-access-token)
        - Inject `asana.token` into the environment so that Spring Boot pulls it in. NEVER check the token value into source control.
        - Two methods have been tested:
            - Pass it in as an argLine to Maven `./mvnw -DargLine="-Dasana.token=<token>" test`.
            - Set it as an environment variable in your shell of choice `export ASANA_TOKEN=<token>`
        - These tokens often contain a colon (`:`) which needs to be escaped with a backslash (`\`) in both of the above examples.
    1. Edit the `application.properties` file to contain the following properties:
        - `asana.workspace.name` - The name of the workspace to be used for reading and importing data.
        - `asana.project.name` - The name of the project which will contain all of the Grant information from [Instrumentl](https://www.instrumentl.com/).
1. Run the Maven tests.
    - `$> ./mvnw test`

