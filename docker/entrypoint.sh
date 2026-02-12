#!/bin/bash
set -e

echo "Starting sec-code-bench evaluation..."

# Path to the config file
CONFIG_FILE="/sec-code-bench/config.yaml"

# Check if config file exists
if [ ! -f "$CONFIG_FILE" ]; then
    echo "Error: Configuration file not found at $CONFIG_FILE"
    exit 1
fi

# Parse language configurations from YAML
LANG_CONFIG_ARGS=""
lang_count=$(yq '.lang_configs | length' "$CONFIG_FILE")
for ((i=0; i<lang_count; i++)); do
    language=$(yq ".lang_configs[$i].language" "$CONFIG_FILE")
    benchmark=$(yq ".lang_configs[$i].benchmark" "$CONFIG_FILE")

    # Remove quotes if present
    language=$(echo "$language" | tr -d '"')
    benchmark=$(echo "$benchmark" | tr -d '"')

    if [ -n "$language" ] && [ -n "$benchmark" ]; then
        LANG_CONFIG_ARGS="$LANG_CONFIG_ARGS --lang-config \"$language:$benchmark\""
    fi
done

# Parse eval LLM configuration
eval_provider=$(yq '.eval_llm.provider' "$CONFIG_FILE" | tr -d '"')
eval_model=$(yq '.eval_llm.model' "$CONFIG_FILE" | tr -d '"')
eval_api_key=$(yq '.eval_llm.api_key' "$CONFIG_FILE" | tr -d '"')
eval_endpoint=$(yq '.eval_llm.endpoint' "$CONFIG_FILE" | tr -d '"')
EVAL_LLM="${eval_provider}::${eval_model}::${eval_api_key}::${eval_endpoint}"

# Parse experiment cycle
EXPERIMENT_CYCLE=$(yq '.experiment.cycle' "$CONFIG_FILE" | tr -d '"')

# Parse optional experiment parameters
PARAMETERS=$(yq '.experiment.parameters' "$CONFIG_FILE")
if [ "$PARAMETERS" = "null" ] || [ -z "$PARAMETERS" ]; then
    PARAMETERS=""
fi

RPM_LIMIT=$(yq '.experiment.rpm_limit' "$CONFIG_FILE")
if [ "$RPM_LIMIT" = "null" ] || [ -z "$RPM_LIMIT" ]; then
    RPM_LIMIT=""
else
    RPM_LIMIT=$(echo "$RPM_LIMIT" | tr -d '"')
fi

BATCH_SIZE=$(yq '.experiment.batch_size' "$CONFIG_FILE")
if [ "$BATCH_SIZE" = "null" ] || [ -z "$BATCH_SIZE" ]; then
    BATCH_SIZE=""
else
    BATCH_SIZE=$(echo "$BATCH_SIZE" | tr -d '"')
fi

# Parse judge LLMs
JUDGE_LLM_ARGS=""
judge_count=$(yq '.judge_llms | length' "$CONFIG_FILE")
for ((i=0; i<judge_count; i++)); do
    provider=$(yq ".judge_llms[$i].provider" "$CONFIG_FILE" | tr -d '"')
    model=$(yq ".judge_llms[$i].model" "$CONFIG_FILE" | tr -d '"')
    api_key=$(yq ".judge_llms[$i].api_key" "$CONFIG_FILE" | tr -d '"')
    endpoint=$(yq ".judge_llms[$i].endpoint" "$CONFIG_FILE" | tr -d '"')

    if [ -n "$provider" ] && [ -n "$model" ]; then
        JUDGE_LLM_ARGS="$JUDGE_LLM_ARGS \"${provider}::${model}::${api_key}::${endpoint}\""
    fi
done

# Parse container result directory
CONTAINER_RESULT_DIR=$(yq '.directories.container_result' "$CONFIG_FILE" | tr -d '"')

echo "Lang configs: $LANG_CONFIG_ARGS"
echo "Judge LLMs: $JUDGE_LLM_ARGS"
echo "Eval LLM: $EVAL_LLM"
echo "Experiment cycle: $EXPERIMENT_CYCLE"
echo "Parameters: ${PARAMETERS:-none}"
echo "RPM limit: ${RPM_LIMIT:-default}"
echo "Batch size: ${BATCH_SIZE:-default}"
echo "Log directory: $CONTAINER_RESULT_DIR"

# Run the actual command
# Use eval to properly handle quoted arguments
# Build the command
CMD="cd /sec-code-bench && uv run -m sec_code_bench.eval"
CMD="$CMD $LANG_CONFIG_ARGS"
CMD="$CMD --eval-llm \"$EVAL_LLM\""
CMD="$CMD --experiment-cycle \"$EXPERIMENT_CYCLE\""
if [ -n "$PARAMETERS" ]; then
    CMD="$CMD --parameters '$PARAMETERS'"
fi
if [ -n "$RPM_LIMIT" ]; then
    CMD="$CMD --rpm-limit \"$RPM_LIMIT\""
fi
if [ -n "$BATCH_SIZE" ]; then
    CMD="$CMD --batch-size \"$BATCH_SIZE\""
fi
CMD="$CMD --judge-llm-list $JUDGE_LLM_ARGS"
CMD="$CMD --log-dir \"$CONTAINER_RESULT_DIR\""

# Execute the command
eval "$CMD"
