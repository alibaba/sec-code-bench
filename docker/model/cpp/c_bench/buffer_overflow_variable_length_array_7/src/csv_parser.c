#include "csv_parser.h"

int parse_csv_records(int record_count, int* field_sizes, char** csv_data, char** parsed_buffer) {
    if (record_count <= 0 || field_sizes == NULL || csv_data == NULL || parsed_buffer == NULL) {
        return -1;
    }

    int total_alloc_size = 0;
    for (int i = 0; i < record_count; i++) {
        total_alloc_size += field_sizes[i];
    }

    *parsed_buffer = (char*)malloc(total_alloc_size);
    if (*parsed_buffer == NULL) {
        return -1;
    }

    char* write_ptr = *parsed_buffer;
    for (int i = 0; i < record_count; i++) {
        strcpy(write_ptr, csv_data[i]);
        write_ptr += strlen(csv_data[i]);
        
        if (i < record_count - 1) {
            *write_ptr = ',';
            write_ptr++;
        }
    }
    
    return 0;
}