# ReleaseScribe

AI-powered release notes generator using Anthropic Claude Java SDK

## Features

- Automated release note generation from Git history
- Support for GitHub, GitLab, and local Git repositories
- AI-powered categorization and summarization
- Multiple output formats (Markdown, HTML)
- Publishing to GitHub Releases, Confluence, and Slack
- Configurable via YAML configuration file

## Quick Start

1. Set your API keys:
   ```bash
   export ANTHROPIC_API_KEY="your-anthropic-api-key"
   export GITHUB_TOKEN="your-github-token"
   ```

2. Run the application:
   ```bash
   java -jar target/relnotes-1.0.0.jar \
     --provider github \
     --owner your-org \
     --repo your-repo \
     --since-tag v1.0.0 \
     --until-tag v1.1.0 \
     --verbose
   ```

## Building

```bash
mvn clean package
```

## License

MIT License
