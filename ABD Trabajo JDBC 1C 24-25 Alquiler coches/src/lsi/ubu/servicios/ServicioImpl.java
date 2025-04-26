package lsi.ubu.servicios;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lsi.ubu.excepciones.AlquilerCochesException;
import lsi.ubu.util.PoolDeConexiones;

public class ServicioImpl implements Servicio {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicioImpl.class);

	private static final int DIAS_DE_ALQUILER = 4;

	public void alquilar(String nifCliente, String matricula, Date fechaIni, Date fechaFin) throws SQLException {
		PoolDeConexiones pool = PoolDeConexiones.getInstance();

		Connection con = null;
		PreparedStatement st = null;
		ResultSet rs = null;
		boolean nula = false;

		// Si se pasa la fechaFin, se calcula la diferencia en días.
        // Si no se pasa, se calcula fechaFin sumándole (DIAS_DE_ALQUILER - 2) días a fechaIni.
        long diasDiff;
        if (fechaFin != null) {
        	nula = false;
            diasDiff = TimeUnit.MILLISECONDS.toDays(fechaFin.getTime() - fechaIni.getTime());
            if (diasDiff < 1) {
                throw new AlquilerCochesException(AlquilerCochesException.SIN_DIAS);
            }
        } else {
            // Se deja diasDiff en DIAS_DE_ALQUILER para la facturación
        	nula = true;
            diasDiff = DIAS_DE_ALQUILER;
            // Se calcula la fecha fin a partir de fechaIni
            fechaFin = new java.sql.Date(fechaIni.getTime() + ((long)(DIAS_DE_ALQUILER - 2)) * 24L * 3600L * 1000L);
        }

		try {
			con = pool.getConnection();

			/* A completar por el alumnado... */

			/* ================================= AYUDA R�PIDA ===========================*/
			/*
			 * Algunas de las columnas utilizan tipo numeric en SQL, lo que se traduce en
			 * BigDecimal para Java.
			 * 
			 * Convertir un entero en BigDecimal: new BigDecimal(diasDiff)
			 * 
			 * Sumar 2 BigDecimals: usar metodo "add" de la clase BigDecimal
			 * 
			 * Multiplicar 2 BigDecimals: usar metodo "multiply" de la clase BigDecimal
			 *
			 * 
			 * Paso de util.Date a sql.Date java.sql.Date sqlFechaIni = new
			 * java.sql.Date(sqlFechaIni.getTime());
			 *
			 *
			 * Recuerda que hay casos donde la fecha fin es nula, por lo que se debe de
			 * calcular sumando los dias de alquiler (ver variable DIAS_DE_ALQUILER) a la
			 * fecha ini.
			 */

			// 1. Comprobamos que el vehículo existe.
            st = con.prepareStatement("SELECT COUNT(*) FROM VEHICULOS WHERE MATRICULA = ?");
            st.setString(1, matricula);
            rs = st.executeQuery();
            rs.next();
            if (rs.getInt(1) == 0) {
                throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_NO_EXIST);
            }
            rs.close();
            st.close();

			// 2. Comprobamos que el cliente existe.
            String sqlCheckCliente = "SELECT 1 FROM CLIENTES WHERE NIF = ?";
            st = con.prepareStatement(sqlCheckCliente);
            st.setString(1, nifCliente);
            rs = st.executeQuery();
            if (!rs.next()) {
                throw new AlquilerCochesException(AlquilerCochesException.CLIENTE_NO_EXIST);
            }
            rs.close();
            st.close();

            // 3. Verificamos nuevamente que la cantidad de días sea válida.
            if (diasDiff < 1) {
                throw new AlquilerCochesException(AlquilerCochesException.SIN_DIAS);
            }

			// 4. Comprobamos disponibilidad del vehículo (no hay solape con otra reserva).
            // La condición de solape es: (nueva_fechaIni <= FECHA_FIN_existente) AND (nueva_fechaFin >= FECHA_INI_existente)
            st = con.prepareStatement(
                "SELECT COUNT(*) FROM RESERVAS " +
                "WHERE MATRICULA = ? " +
                "  AND ? <= FECHA_FIN " +
                "  AND ? >= FECHA_INI"
            );
            st.setString(1, matricula);
            st.setDate(2, new java.sql.Date(fechaIni.getTime()));
            st.setDate(3, new java.sql.Date(fechaFin.getTime()));
            rs = st.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                throw new AlquilerCochesException(AlquilerCochesException.VEHICULO_OCUPADO);
            }
            rs.close();
            st.close();

		} catch (SQLException e) {
			// Completar por el alumno

			LOGGER.debug(e.getMessage());

			throw e;

		} finally {
			/* A rellenar por el alumnado*/
		}
	}
}
