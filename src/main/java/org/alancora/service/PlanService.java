package org.alancora.service;

import java.sql.SQLException;
import java.util.List;

import org.alancora.dao.PlanDao;
import org.alancora.dao.UserDao;
import org.alancora.model.Emergency;
import org.alancora.model.Plan;
import org.alancora.model.User;

public final class PlanService {
    private final UserDao users;
    private final PlanDao plans;

    public PlanService(final UserDao users, final PlanDao plans) {
        this.users = users;
        this.plans = plans;
    }

    public Plan create(final long userId, final String code) throws SQLException {
        final User user = user(userId);
        final Emergency emergency = Emergency.parse(code);
        return plans.create(user, emergency, PlanEngine.VERSION, PlanEngine.generate(user, emergency));
    }

    public List<Plan> list(final long userId) throws SQLException {
        user(userId);
        return plans.list(userId);
    }

    public Plan find(final long userId, final long id) throws SQLException {
        user(userId);
        return plans.find(userId, id);
    }

    public Plan update(final long userId, final long id, final String code) throws SQLException {
        final User user = user(userId);
        final Emergency emergency = Emergency.parse(code);
        return plans.update(id, user, emergency, PlanEngine.VERSION, PlanEngine.generate(user, emergency));
    }

    public boolean delete(final long userId, final long id) throws SQLException {
        user(userId);
        return plans.delete(userId, id);
    }

    private User user(final long id) throws SQLException {
        final User user = users.findById(id);
        if (user == null) {
            throw new SecurityException("Entre na sua conta para continuar.");
        }
        return user;
    }
}
