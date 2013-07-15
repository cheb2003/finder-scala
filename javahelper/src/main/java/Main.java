import com.jolbox.bonecp.BoneCPDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 */
public class Main {
    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        BoneCPDataSource ds = new BoneCPDataSource();
        Class.forName("org.h2.Driver");
        ds.setJdbcUrl("jdbc:h2:tcp://localhost/~/my");
        ds.setUsername("sa");
        ds.setPassword("");
        Connection conn = ds.getConnection();
        PreparedStatement ps = conn.prepareStatement("insert into qdw_tb_producttitlesegmentword (keyid_bigint , productkeyid_nvarchar , languagecode_nvarchar , producttitlecn_nvarchar , searchkeyword_nvarchar , segmentword_nvarchar )\n" +
                "values(1,‘A905600AHR’,‘ru’,‘Shiseido资生堂 洗颜专科柔澈泡沫洁面乳’,‘Shiseido’,‘sss|fdfdf|大家’)");
    }
}
