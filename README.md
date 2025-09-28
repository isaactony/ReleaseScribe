# ReleaseScribe

AI-powered release, changelog, customer highlights, and upgrade instructions management platform that automates the entire release documentation workflow powered by Anthropic Claude.

## What ReleaseScribe Does

ReleaseScribe is a comprehensive release management tool that transforms your Git history into professional release documentation. It goes beyond simple note generation to provide:

### Core Capabilities
- **Intelligent Change Analysis**: Analyzes commits and pull requests to understand what actually changed
- **Multi-Audience Documentation**: Generates different formats for developers, customers, and stakeholders
- **Automated Categorization**: Uses AI to categorize changes into features, fixes, security updates, and breaking changes
- **Component Detection**: Automatically identifies which parts of your codebase were affected
- **Professional Formatting**: Creates publication-ready documentation in multiple formats

### Documentation Types Generated
- **Technical Changelogs**: Detailed developer-focused change logs following "Keep a Changelog" standards
- **Engineering Release Notes**: Internal documentation for development teams
- **Customer Highlights**: User-friendly summaries of new features and improvements
- **Breaking Changes**: Detailed migration guides for breaking changes
- **Upgrade Instructions**: Step-by-step upgrade guidance

## Features

- **Multi-Platform Support**: GitHub, GitLab, and local Git repositories
- **AI-Powered Intelligence**: Uses Claude to understand context and categorize changes meaningfully
- **Flexible Publishing**: Output to files, GitHub Releases, Confluence, or Slack
- **Configurable Workflows**: Customize categorization, formatting, and publishing via YAML
- **CI/CD Integration**: Ready-to-use Jenkins and GitHub Actions workflows
- **Smart Filtering**: Handles large repositories with intelligent change filtering

## Quick Start

1. **Set your API keys:**
   ```bash
   export ANTHROPIC_API_KEY="your-anthropic-api-key"
   export GITHUB_TOKEN="your-github-token"  # For GitHub integration
   ```

2. **Generate release documentation:**
   ```bash
   java -jar target/relnotes-1.0.0.jar \
     --provider github \
     --owner your-org \
     --repo your-repo \
     --since-tag v1.0.0 \
     --until-tag v1.1.0 \
     --verbose
   ```

3. **Output files created:**
   - `dist/CHANGELOG.md` - Technical changelog for developers
   - `dist/RELEASE_NOTES.md` - Engineering release notes
   - `dist/HIGHLIGHTS.md` - Customer-facing highlights
   - `dist/RELEASE_BODY.md` - Complete release documentation

## Advanced Usage

### Publishing to GitHub Releases
```bash
export GITHUB_TOKEN="your-token"
java -jar target/relnotes-1.0.0.jar \
  --provider github \
  --owner microsoft \
  --repo vscode \
  --since-date "2024-01-01" \
  --until-date "2024-01-31" \
  --publish github \
  --release-tag v1.85.0
```

### Publishing to Confluence
```bash
export CONFLUENCE_URL="https://your-domain.atlassian.net"
export CONFLUENCE_TOKEN="your-token"
java -jar target/relnotes-1.0.0.jar \
  --provider github \
  --owner your-org \
  --repo your-repo \
  --since-tag v1.0.0 \
  --publish confluence \
  --confluence-space "PROD"
```

### Custom Configuration
Create a `.relnotes.yml` file to customize categorization, formatting, and component detection:

```yaml
version: 1
sections:
  - features
  - fixes
  - security
  - breaking
conventions:
  componentPaths:
    api: ["service-api/**", "src/main/java/com/example/api/**"]
    ui: ["web/**", "frontend/**"]
labelMapping:
  feature: ["feature", "enhancement", "feat"]
  fix: ["bug", "fix", "bugfix"]
```

## Building

```bash
mvn clean package
```

## CI/CD Integration

### GitHub Actions
```yaml
- name: Generate Release Notes
  env:
    ANTHROPIC_API_KEY: ${{ secrets.ANTHROPIC_API_KEY }}
    GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
  run: |
    java -jar target/relnotes-1.0.0.jar \
      --provider github \
      --owner ${{ github.repository_owner }} \
      --repo ${{ github.event.repository.name }} \
      --since-tag ${{ github.event.release.tag_name }} \
      --publish github
```

### Jenkins Pipeline
See the included `Jenkinsfile` for a complete pipeline example.

## License

MIT License
