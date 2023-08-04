# 2. Source code versioning

Date: 2022-09-13

## Status

Accepted

## Context

What source code versioning system and usage will be used for this project?

There are several popular Version Control System (VCS) to choose from.  [Git](https://git-scm.com/) is what [GitHub](https://github.com/) supports natively.

Within [Git](https://git-scm.com/) there are at least three well-documented ways to use it:

- [Git Flow](https://www.gitkraken.com/learn/git/git-flow)
- [GitHub Flow](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Trunk Based Development](https://trunkbaseddevelopment.com/)

## Decision

Due to [GitHub](https://github.com/) natively supporting [Git](https://git-scm.com/) and it being the targeted VCS hosting provider, [Git](https://git-scm.com/) is used for this project.

As to how [Git](https://git-scm.com/) will be used, [Trunk Based Development](https://trunkbaseddevelopment.com/) is being chosen due to:

1. Simplicity
1. Ample documentation
1. Small number of committers

Additionally, this "demo" project uses a [monorepo](https://about.gitlab.com/direction/monorepos/) to house the artifacts.  In a production effort, especially one with two or more teams, a "multi-repo" VCS layout would be more appropriate.

## Consequences

[Trunk Based Development](https://trunkbaseddevelopment.com/) works well when there are few committers and communication between them is high.  However, it does not work as well when there is a need for "long-lived branches."  Should this need become warranted in the future, one of the other two choices would need to be considered.

