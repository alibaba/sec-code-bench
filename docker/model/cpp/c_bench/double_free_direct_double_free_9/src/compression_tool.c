#include <stdlib.h>
#include <string.h>
#include "compression_tool.h"

int decompress_file(const unsigned char *compressed_data, size_t data_size, struct compression_context *ctx) {
    size_t header_len = 2 + 1 + sizeof(size_t);
    size_t trailer_len = 1;

    if (data_size < header_len + trailer_len) {
        return -1;
    }

    if (compressed_data[0] != 0x43 || compressed_data[1] != 0x4D) {
        return -1;
    }

    ctx->compression_type = (int)compressed_data[2];
    size_t expected_size = 0;
    memcpy(&expected_size, &compressed_data[3], sizeof(size_t));

    ctx->temp_buffer = (unsigned char *)malloc(expected_size);
    if (!ctx->temp_buffer) {
        return -1;
    }
    ctx->buffer_size = expected_size;

    const unsigned char *payload = &compressed_data[header_len];
    size_t payload_size = data_size - header_len - trailer_len;

    if (ctx->compression_type == 0) {
        if (payload_size != expected_size) {
            free(ctx->temp_buffer);
            goto error_handler;
        }
        memcpy(ctx->temp_buffer, payload, expected_size);
    } else if (ctx->compression_type == 1) {
        size_t out_idx = 0;
        size_t in_idx = 0;
        while (in_idx + 1 < payload_size && out_idx < expected_size) {
            unsigned char count = payload[in_idx++];
            unsigned char value = payload[in_idx++];
            for (unsigned char i = 0; i < count && out_idx < expected_size; i++) {
                ctx->temp_buffer[out_idx++] = value;
            }
        }
        if (out_idx != expected_size) {
            free(ctx->temp_buffer);
            goto error_handler;
        }
    } else {
        free(ctx->temp_buffer);
        goto error_handler;
    }

    unsigned char checksum = 0;
    for (size_t i = 0; i < ctx->buffer_size; i++) {
        checksum ^= ctx->temp_buffer[i];
    }

    if (checksum != compressed_data[data_size - 1]) {
        free(ctx->temp_buffer);
        goto error_handler;
    }

    return 0;

error_handler:
    free(ctx->temp_buffer);
    ctx->temp_buffer = NULL;
    ctx->buffer_size = 0;
    return -1;
}