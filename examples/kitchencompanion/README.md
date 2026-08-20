# Kitchen Companion
Tool to organize and manage the drinks and snacks tally sheets in the SE kitchen.

## Starting
Start the backend with `gradle :examples:kitchencompanion:backend:runServer` and the frontend with `gradle :examples:kitchencompanion:frontend:runApp`.
The App is accessible at `http://localhost:4200` and the default Account is `Alice` and `a`.

## Admin password
Deployments must set the environment variable `adminPwHash` to a BCrypt hash. When `deploy=true`
and `adminPwHash` is unset.
A hash can be generated with any BCrypt tool, e.g. `htpasswd -bnBC 10 "" <password>`.
