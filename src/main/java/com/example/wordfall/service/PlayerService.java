package com.example.wordfall.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.wordfall.entity.Player;
import com.example.wordfall.exception.NotFoundException;
import com.example.wordfall.repository.PlayerRepository;

@Service
public class PlayerService {

    private final PlayerRepository players;
    private final TransactionTemplate newTransaction;

    public PlayerService(PlayerRepository players, PlatformTransactionManager transactionManager) {
        this.players = players;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * プレイヤー行が無ければ作る。別タブから同時に作られても失敗しないよう、
     * 独立したトランザクションで作成し、一意制約違反は「既にある」とみなす。
     */
    public void ensureExists(UUID playerId) {
        if (players.existsById(playerId)) return;
        try {
            newTransaction.executeWithoutResult(status ->
                    players.saveAndFlush(new Player(playerId, LocalDateTime.now())));
        } catch (DataIntegrityViolationException alreadyCreated) {
            if (!players.existsById(playerId)) throw alreadyCreated;
        }
    }

    /** 呼び出し元のトランザクションが終わるまで、このプレイヤーの他の書き込みを待たせる。 */
    public Player lock(UUID playerId) {
        return players.lockById(playerId).orElseThrow(() -> new NotFoundException("Player not found"));
    }
}
