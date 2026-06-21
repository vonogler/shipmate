FROM ubuntu:24.04

ARG RUNNER_VERSION=2.328.0

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
      ca-certificates \
      curl \
      git \
      jq \
      libicu74 \
      tar \
      unzip \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --create-home --shell /bin/bash runner

WORKDIR /home/runner/actions-runner

RUN curl -fsSL "https://github.com/actions/runner/releases/download/v${RUNNER_VERSION}/actions-runner-linux-x64-${RUNNER_VERSION}.tar.gz" \
    | tar -xz \
    && ./bin/installdependencies.sh

COPY shipmate-infra/github-runner/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh \
    && chown -R runner:runner /home/runner/actions-runner

USER runner

ENTRYPOINT ["/entrypoint.sh"]
