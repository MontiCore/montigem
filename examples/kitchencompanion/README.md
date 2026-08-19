# Kitchen Companion
Tool to organize and manage the drinks and snacks tally sheets in the SE kitchen.

## Starting
Start the backend with `gradle :examples:kitchencompanion:backend:runServer` and the frontend with `gradle :examples:kitchencompanion:frontend:runApp`.
The App is accessible at `http://localhost:4200` and the default Account is `Alice` and `a`.

## Admin password
The admin login is verified on the backend against a BCrypt hash; no password or hash is part of
the frontend bundle. Locally the dev password `admin1234` applies.

Deployments must set the environment variable `adminPwHash` to a BCrypt hash. When `deploy=true`
and `adminPwHash` is unset, the admin login is disabled instead of falling back to the dev password.
A hash can be generated with any BCrypt tool, e.g. `htpasswd -bnBC 10 "" <password>`.
