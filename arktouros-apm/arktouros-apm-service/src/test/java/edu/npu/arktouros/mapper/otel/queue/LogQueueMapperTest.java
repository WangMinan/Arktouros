package edu.npu.arktouros.mapper.otel.queue;

import edu.npu.arktouros.model.queue.LogQueueItem;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * @author : [wangminan]
 * @description : {@link LogQueueMapper}
 */
@SpringBootTest
@ExtendWith({SpringExtension.class, MockitoExtension.class})
@Slf4j
@MockitoSettings(strictness = Strictness.LENIENT)
@Timeout(30)
class LogQueueMapperTest {

    @MockBean
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Resource
    private LogQueueMapper logQueueMapper;

    @BeforeEach
    void beforeEach() throws SQLException {
        Mockito.when(dataSource.getConnection()).thenReturn(connection);
    }

    @Test
    void testAdd() throws SQLException {
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(anyString(), anyInt()))
                .thenReturn(preparedStatement);
        try (ResultSet rs = Mockito.mock(ResultSet.class)) {
            Mockito.when(preparedStatement.getGeneratedKeys()).thenReturn(rs);
            Mockito.when(rs.getLong("ID")).thenReturn(1L);
            LogQueueItem item = LogQueueItem.builder().build();
            logQueueMapper.add(item);
            Assertions.assertEquals(1L, item.getId());
        }
    }

    @Test
    void testAddError() throws SQLException {
        Mockito.when(connection.prepareStatement(anyString(), anyInt()))
                .thenThrow(new SQLException());
        Assertions.assertDoesNotThrow(() -> logQueueMapper.add(LogQueueItem.builder().build()));
    }

    @Test
    void testGetTop() throws SQLException {
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        try (ResultSet rs = Mockito.mock(ResultSet.class)) {
            Mockito.when(preparedStatement.executeQuery()).thenReturn(rs);
            Mockito.when(rs.next()).thenReturn(true, false);
            Mockito.when(rs.getLong("ID")).thenReturn(1L);
            Mockito.when(rs.getString("DATA")).thenReturn("data");
            LogQueueItem top = logQueueMapper.getTop();
            Assertions.assertEquals(top.getId(), 1L);
        }
    }

    @Test
    void testGetTopNull() throws SQLException {
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        try (ResultSet rs = Mockito.mock(ResultSet.class)) {
            Mockito.when(preparedStatement.executeQuery()).thenReturn(rs);
            Mockito.when(rs.next()).thenReturn(false);
            Assertions.assertNull(logQueueMapper.getTop());
        }
    }

    @Test
    void getTopError() throws SQLException {
        Mockito.when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException());
        Assertions.assertDoesNotThrow(() -> logQueueMapper.getTop());
    }

    @Test
    void getSize() throws SQLException {
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        try (ResultSet rs = Mockito.mock(ResultSet.class)) {
            Mockito.when(preparedStatement.executeQuery()).thenReturn(rs);
            Mockito.when(rs.next()).thenReturn(true);
            Mockito.when(rs.getLong(1)).thenReturn(1L);
            Assertions.assertFalse(logQueueMapper.isEmpty());
        }
    }

    @Test
    void getSizeError() throws SQLException {
        Mockito.when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException());
        Assertions.assertEquals(0, logQueueMapper.getSize());
    }

    @Test
    void testRemoveTop() throws SQLException {
        PreparedStatement preparedStatement = Mockito.mock(PreparedStatement.class);
        Mockito.when(connection.prepareStatement(anyString()))
                .thenReturn(preparedStatement);
        Assertions.assertDoesNotThrow(() -> logQueueMapper.removeTop());
    }

    @Test
    void testRemoveTopError() throws SQLException {
        Mockito.when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException());
        Assertions.assertDoesNotThrow(() -> logQueueMapper.removeTop());
    }

    @Test
    void TestPrepareTableError() throws SQLException {
        Mockito.when(connection.prepareStatement(anyString()))
                .thenThrow(new SQLException());
        Assertions.assertFalse(logQueueMapper.prepareTable());
    }
}
