# AGENTS.md

## Cursor Cloud specific instructions

This is a minimal Node.js scaffold project with zero external dependencies.

### Running the application

```
node index.js
```

### Key notes

- The project manifest is `project_config.json` (not `package.json`). There is no `package.json`, so `npm install` / `npm start` will not work without first renaming or creating one.
- There are no external dependencies — no `node_modules` needed.
- There is no test framework configured; `npm test` (if a `package.json` were present) would just echo an error placeholder.
- There is no lint configuration. No ESLint, Prettier, or similar tools are set up.
- There is no build step. The application is a single `index.js` that runs directly with Node.js.
