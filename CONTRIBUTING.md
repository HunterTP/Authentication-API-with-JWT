# Contributing to Authentication API with JWT

Thank you for your interest in contributing!

## Ways to Contribute

There are many ways to contribute to this project:

- Report bugs
- Suggest new features
- Improve documentation
- Submit pull requests
- Share the project

## Development Setup

### Prerequisites

- Java 17 or higher
- Maven 3.9+
- MySQL 8.0+ (or Docker)

### Local Development

```bash
# Clone the repository
git clone https://github.com/HunterTP/Authentication-API-with-JWT.git
cd Authentication-API-with-JWT

# Build
mvn clean package

# Run tests
mvn test
```

### Docker Development

```bash
# Start services
docker compose up -d

# View logs
docker compose logs -f
```

## Code Style

- Use 4 spaces for indentation
- Follow existing code conventions in the project
- Add Javadoc comments for public APIs
- Keep methods focused and small

## Pull Request Process

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Run tests and ensure they pass
5. Commit with clear commit messages
6. Push to your fork
7. Submit a Pull Request

## Reporting Bugs

When reporting bugs, please include:

- Clear description of the issue
- Steps to reproduce
- Expected behavior
- Environment details (OS, Java version, etc.)

## Requesting Features

When requesting features, please describe:

- The problem you're trying to solve
- Your proposed solution
- Alternative solutions you've considered

## License

By contributing, you agree that your contributions will be licensed under the [MIT License](LICENSE).