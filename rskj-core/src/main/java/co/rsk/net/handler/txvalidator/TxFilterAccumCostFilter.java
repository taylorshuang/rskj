/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.net.handler.txvalidator;

import co.rsk.net.handler.TxsPerAccount;
import org.ethereum.core.AccountState;
import org.ethereum.core.Block;
import org.ethereum.core.Transaction;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.List;

/**
 * Checks if the sum cost of all transactions of a given account considering
 * gaslimit, gasprice and value can be covered by the account balance in nonce
 * order
 */
public class TxFilterAccumCostFilter implements TxFilter {

    @Override
    public List<Transaction> filter(AccountState state, TxsPerAccount tpa, Block block) {
        BigInteger accumTxCost = BigInteger.valueOf(0);

        tpa.getTransactions().sort((t1, t2) -> {
            BigInteger n1 = new BigInteger(1, t1.getNonce());
            BigInteger n2 = new BigInteger(1, t2.getNonce());
            return n1.compareTo(n2);
        });

        List<Transaction> newTxs = new LinkedList<>();

        for (Transaction t : tpa.getTransactions()) {
            BigInteger gasCost = BigInteger.ZERO;
            if (block == null || t.transactionCost(block) > 0) {
                gasCost = new BigInteger(1, t.getGasLimit()).multiply(new BigInteger(1, t.getGasPrice()));
            }

            if (accumTxCost.add(gasCost).compareTo(state.getBalance()) > 0) {
                break;
            }
            accumTxCost = accumTxCost.add(gasCost);
            accumTxCost = accumTxCost.add(new BigInteger(1, t.getValue()));
            newTxs.add(t);
        }
        return newTxs;
    }

}
