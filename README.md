# Confession Bot

A Discord bot written in Kotlin that allows users to send anonymous confessions in configured channels.

## Features

- Send anonymous confessions via direct messages
- Configure confession channels for servers
- Slash command support
- Confession logging
- Server configuration persistence

## Tech Stack

- Kotlin
- JDA (Java Discord API)
- Gradle
- OkHttp for HTTP requests
- Gson for JSON handling
- Supabase for data storage
- Koin for Dependency Injection

## Setup

1. Clone the repository
2. Create a `.env` file in the root directory with the following variables:
   ```
   BOT_TOKEN=your_discord_bot_token
   DATABASE_SERVER_URL=your_supabase_url
   API_KEY=your_supabase_api_key
   ```
3. Build the project:
   ```bash
   ./gradlew build
   ```
4. Run the bot:
   ```bash
   ./gradlew run
   ```

Alternatively, you can use Docker:
```bash
docker build -t confession-bot .
docker run -d --env-file .env confession-bot
```

## Commands

### Text Commands
- `!hi` - Display bot information
- `!c <message>` - Send an anonymous confession (only works in DMs)
- `!configure` - Set the current channel as the confession channel
- `!channel <channel-id>` - Set a specific channel as the confession channel

### Slash Commands
- `/confess message:<text>` - Send a confession anonymously
- `/configure channel:<channel>` - Configure the confession channel
- `/remove` - Remove the configured confession channel

## Project Structure

- `src/main/kotlin/`
  - `ConfessionBot.kt` - Main bot class handling Discord events
  - `ServerCommandHandler.kt` - Command handling logic
  - `constants/` - Bot constants and messages
  - `di/` - Dependency injection setup using Koin
  - `models/` - Data classes for logs and configurations
  - `repository/` -
    - `RemoteService.kt` - Handles all remote API interactions
    - `LogService.kt` - Wrapper for logging operations
    - `ServerConfigRepository.kt` - Wrapper for server configuration operations

## Refactored Architecture

- **RemoteService**: Primary handler for all remote API interactions.
- **Repositories**: Wrappers over `RemoteService` to handle specific business logic:
  - `LogService`: Handles logging operations.
  - `ServerConfigRepository`: Manages server configuration operations.
- **Dependency Injection**: Koin is used to manage dependencies and decouple components.

## TODO

- [ ] **Code Refactoring**: Ensure all classes and methods adhere to clean code principles.
- [ ] **Lint Checks**: Integrate a Kotlin linter (e.g., `ktlint`) to enforce coding standards.
- [ ] **Unit Tests**: Add unit tests for all modules using JUnit or KotlinTest.
- [ ] **Integration Tests**: Test the interaction between different modules and external APIs.
- [ ] **Error Handling**: Review and improve error handling across the application.
- [ ] **Documentation**: Enhance inline comments and external documentation for better clarity.
- [ ] **CI/CD Pipeline**: Set up a continuous integration pipeline to automate testing and deployment.

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.