package ru.yandex.practicum.accounts.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.accounts.model.Account;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findAllByLoginNot(String login);
}