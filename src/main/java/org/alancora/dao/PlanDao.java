package org.alancora.dao;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.alancora.config.Database;
import org.alancora.model.Emergency;
import org.alancora.model.Orientation;
import org.alancora.model.Plan;
import org.alancora.model.User;

public class PlanDao {
    private static final String COLUMNS = "id, usuario_id, criado_em, versao_regras, emergencia, "
            + "nome, moradia, localizacao_aproximada, tipo_moradia, necessidades";

    public Plan create(final User user, final Emergency emergency, final int version,
            final List<Orientation> items) throws SQLException {
        final String sql = "INSERT INTO plano (usuario_id, versao_regras, emergencia, nome, moradia, "
                + "localizacao_aproximada, tipo_moradia, necessidades) VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING "
                + COLUMNS;

        try (final Connection connection = Database.connect()) {
            connection.setAutoCommit(false);

            try (final PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setLong(1, user.getId());
                statement.setInt(2, version);
                statement.setString(3, emergency.name());
                statement.setString(4, user.getName());
                statement.setString(5, user.getHousing());
                statement.setString(6, user.getLocation());
                statement.setString(7, user.getHousingType());

                final Array needs = connection.createArrayOf("text",
                        user.getNeeds().stream().distinct().sorted().toArray(String[]::new));

                final Plan plan;

                try {
                    statement.setArray(8, needs);

                    try (final ResultSet result = statement.executeQuery()) {
                        if (!result.next()) {
                            throw new SQLException("Não foi possível salvar o plano.");
                        }

                        plan = read(result, items);
                    }
                } finally {
                    needs.free();
                }

                saveOrientations(connection, plan.id(), items);

                connection.commit();
                return plan;
            } catch (final SQLException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    public List<Plan> list(final long userId) throws SQLException {
        final String sql = "SELECT " + COLUMNS + " FROM plano WHERE usuario_id = ? ORDER BY id DESC";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);

            try (final ResultSet result = statement.executeQuery()) {
                final List<Plan> plans = new ArrayList<>();

                while (result.next()) {
                    plans.add(read(result, List.of()));
                }

                return List.copyOf(plans);
            }
        }
    }

    public Plan find(final long userId, final long id) throws SQLException {
        final String sql = "SELECT " + COLUMNS + " FROM plano WHERE usuario_id = ? AND id = ?";

        try (final Connection connection = Database.connect();
                final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            statement.setLong(2, id);

            try (final ResultSet result = statement.executeQuery()) {
                return result.next() ? read(result, orientations(connection, id)) : null;
            }
        }
    }

    private static void saveOrientations(final Connection connection, final long id,
            final List<Orientation> items) throws SQLException {
        final String sql = "INSERT INTO orientacao (plano_id, ordem, regra, motivo, texto, fonte) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < items.size(); index++) {
                final Orientation item = items.get(index);

                statement.setLong(1, id);
                statement.setInt(2, index + 1);
                statement.setString(3, item.rule());
                statement.setString(4, item.reason());
                statement.setString(5, item.text());
                statement.setString(6, item.source());
                statement.addBatch();
            }

            statement.executeBatch();
        }
    }

    private static List<Orientation> orientations(final Connection connection, final long id) throws SQLException {
        final String sql = "SELECT regra, motivo, texto, fonte FROM orientacao WHERE plano_id = ? ORDER BY ordem";

        try (final PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);

            try (final ResultSet result = statement.executeQuery()) {
                final List<Orientation> items = new ArrayList<>();

                while (result.next()) {
                    items.add(new Orientation(result.getString("regra"), result.getString("motivo"),
                            result.getString("texto"), result.getString("fonte")));
                }

                return List.copyOf(items);
            }
        }
    }

    private static Plan read(final ResultSet result, final List<Orientation> items) throws SQLException {
        final Array needs = result.getArray("necessidades");

        try {
            return new Plan(result.getLong("id"), result.getLong("usuario_id"),
                    result.getTimestamp("criado_em").toInstant(), result.getInt("versao_regras"),
                    Emergency.parse(result.getString("emergencia")), result.getString("nome"),
                    result.getString("moradia"), result.getString("localizacao_aproximada"),
                    result.getString("tipo_moradia"),
                    Arrays.asList((String[]) needs.getArray()), items);
        } finally {
            needs.free();
        }
    }
}
