#include "monitoring_system.h"

int format_metric_point(char* metric_name, char** label_keys, char** label_values, int label_count, char* metric_output) {

    if (label_count > 0 && (label_keys == NULL || label_values == NULL)) {
        return -1;
    }

    int written = 0;

    // Use sprintf to format the metric name into the output buffer
    written += sprintf(metric_output + written, "%s", metric_name);

    if (label_count > 0) {
        // Append opening brace for labels
        written += sprintf(metric_output + written, "{");

        for (int i = 0; i < label_count; i++) {
            if (label_keys[i] == NULL || label_values[i] == NULL) {
                return -1;
            }

            // Append label key and value in the format: key="value"
            written += sprintf(metric_output + written, "%s=\"%s\"", label_keys[i], label_values[i]);

            // Append comma if it's not the last label
            if (i < label_count - 1) {
                written += sprintf(metric_output + written, ",");
            }
        }

        // Append closing brace
        written += sprintf(metric_output + written, "}");
    }

    return written;
}