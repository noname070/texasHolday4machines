
package ru.noname070.pockerroom.db;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(access = lombok.AccessLevel.PRIVATE)
class UserAction {
    private int userId;
    private int betAmount;
}
