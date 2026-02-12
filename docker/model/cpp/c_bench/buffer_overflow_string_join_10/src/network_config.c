#include "network_config.h"

int generate_network_config_cmd(char* cmd_buffer, size_t buffer_size, const char* ip_addr, const char* netmask, const char* gateway) {
    if (cmd_buffer == NULL || ip_addr == NULL || netmask == NULL || gateway == NULL) {
        return -1;
    }

    if (strlen(ip_addr) == 0 || strlen(netmask) == 0 || strlen(gateway) == 0) {
        return -1;
    }

    /* Start constructing the command */
    strcpy(cmd_buffer, "ip addr ");

    /* Concatenate IP address */
    strcat(cmd_buffer, ip_addr);

    /* Concatenate netmask part */
    strncat(cmd_buffer, " netmask ", 9);
    strcat(cmd_buffer, netmask);

    /* Concatenate gateway part */
    strcat(cmd_buffer, " gateway ");
    strncat(cmd_buffer, gateway, strlen(gateway));

    return 0;
}