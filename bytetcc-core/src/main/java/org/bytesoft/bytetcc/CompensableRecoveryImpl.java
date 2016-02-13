/**
 * Copyright 2014-2016 yangming.liu<bytefox@126.com>.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, see <http://www.gnu.org/licenses/>.
 */
package org.bytesoft.bytetcc;

import java.util.List;

import javax.transaction.Status;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.log4j.Logger;
import org.bytesoft.bytetcc.aware.CompensableBeanFactoryAware;
import org.bytesoft.compensable.CompensableBeanFactory;
import org.bytesoft.compensable.CompensableManager;
import org.bytesoft.compensable.CompensableTransaction;
import org.bytesoft.compensable.TransactionContext;
import org.bytesoft.compensable.archive.CompensableArchive;
import org.bytesoft.compensable.archive.CompensableResourceArchive;
import org.bytesoft.compensable.supports.logger.CompensableLogger;
import org.bytesoft.transaction.Transaction;
import org.bytesoft.transaction.TransactionRecovery;
import org.bytesoft.transaction.TransactionRepository;
import org.bytesoft.transaction.archive.XAResourceArchive;
import org.bytesoft.transaction.supports.TransactionStatistic;
import org.bytesoft.transaction.xa.TransactionXid;

public class CompensableRecoveryImpl implements TransactionRecovery, CompensableBeanFactoryAware {
	static final Logger logger = Logger.getLogger(CompensableRecoveryImpl.class.getSimpleName());

	private TransactionStatistic transactionStatistic;
	private CompensableBeanFactory beanFactory;

	public CompensableTransaction reconstructTransaction(CompensableArchive archive) {
		TransactionContext transactionContext = new TransactionContext();
		transactionContext.setXid((TransactionXid) archive.getXid());
		transactionContext.setRecoveried(true);
		transactionContext.setCoordinator(archive.isCoordinator());
		transactionContext.setCompensable(archive.isCompensable());

		CompensableTransaction transaction = null;
		if (archive.isCompensable()) {
			SampleCompensableImpl tccTransaction = new SampleCompensableImpl(transactionContext);
			List<CompensableResourceArchive> compensables = archive.getCompensableResourceList();
			for (int i = 0; i < compensables.size(); i++) {
				CompensableResourceArchive compensable = compensables.get(i);
				if (compensable.isCoordinator()) {
					tccTransaction.getCoordinatorResourceArchiveList().add(compensable);
				} else {
					tccTransaction.getParticipantResourceArchiveList().add(compensable);
				}
			}
			List<XAResourceArchive> resources = archive.getRemoteResources();
			for (int i = 0; i < resources.size(); i++) {
				XAResourceArchive resource = resources.get(i);
				Xid xid = resource.getXid();
				tccTransaction.getResourceArchives().put(xid, resource);
			}
			tccTransaction.setTransactionStatus(archive.getStatus());
			tccTransaction.setCompensableStatus(archive.getCompensableStatus());
			transaction = tccTransaction;
		} else {
			transaction = new SampleTransactionImpl(transactionContext);
		}

		if (archive.getVote() == XAResource.XA_RDONLY) {
			throw new IllegalStateException();
		}

		return transaction;
	}

	/**
	 * commit/rollback the uncompleted transactions.
	 */
	public synchronized void startRecovery() {
		TransactionRepository transactionRepository = this.beanFactory.getTransactionRepository();
		CompensableLogger transactionLogger = this.beanFactory.getCompensableLogger();
		List<CompensableArchive> archives = transactionLogger.getTransactionArchiveList();
		for (int i = 0; i < archives.size(); i++) {
			CompensableTransaction transaction = null;
			try {
				CompensableArchive archive = (CompensableArchive) archives.get(i);
				transaction = this.reconstructTransaction(archive);
			} catch (RuntimeException rex) {
				continue;
			}
			TransactionContext transactionContext = transaction.getTransactionContext();
			TransactionXid globalXid = transactionContext.getXid();
			if (SampleCompensableImpl.class.isInstance(transaction)) {
				this.reconstructTccTransaction((SampleCompensableImpl) transaction);
			} else {
				this.reconstructJtaTransaction((SampleTransactionImpl) transaction);
			}
			if (transactionRepository.getTransaction(globalXid) == null) {
				transactionRepository.putTransaction(globalXid, transaction);
				transactionRepository.putErrorTransaction(globalXid, transaction);
			}
		}

	}

	/**
	 * TODO
	 */
	private void reconstructJtaTransaction(SampleTransactionImpl transaction) {
		// org.bytesoft.bytejta.common.TransactionConfigurator jtaConfigurator =
		// org.bytesoft.bytejta.common.TransactionConfigurator
		// .getInstance();
		// org.bytesoft.bytejta.common.TransactionRepository jtaRepository = jtaConfigurator.getTransactionRepository();
		// Xid xid = transaction.getTransactionContext().getCurrentXid();
		// TransactionXid jtaGlobalXid = jtaConfigurator.getXidFactory().createGlobalXid(xid.getGlobalTransactionId());
		// TransactionImpl jtaTransaction = jtaRepository.getErrorTransaction(jtaGlobalXid);
		// if (jtaTransaction != null) {
		// jtaTransaction.registerTransactionListener(transaction);
		// }
	}

	/**
	 * TODO
	 */
	private void reconstructTccTransaction(SampleCompensableImpl transaction) {
		// org.bytesoft.bytejta.common.TransactionConfigurator jtaConfigurator =
		// org.bytesoft.bytejta.common.TransactionConfigurator
		// .getInstance();
		// org.bytesoft.bytejta.common.TransactionRepository jtaRepository = jtaConfigurator.getTransactionRepository();
		// List<CompensableArchive> coordinators = transaction.getCoordinatorArchives();
		// for (int i = 0; i < coordinators.size(); i++) {
		// CompensableArchive archive = coordinators.get(i);
		// Xid xid = archive.getXid();
		// TransactionXid jtaGlobalXid = jtaConfigurator.getXidFactory().createGlobalXid(xid.getGlobalTransactionId());
		// TransactionImpl jtaTransaction = jtaRepository.getErrorTransaction(jtaGlobalXid);
		// if (jtaTransaction != null) {
		// jtaTransaction.registerTransactionListener(transaction);
		// }
		// }
		// List<CompensableArchive> participants = transaction.getParticipantArchives();
		// for (int i = 0; i < participants.size(); i++) {
		// CompensableArchive archive = participants.get(i);
		// Xid xid = archive.getXid();
		// TransactionXid jtaGlobalXid = jtaConfigurator.getXidFactory().createGlobalXid(xid.getGlobalTransactionId());
		// TransactionImpl jtaTransaction = jtaRepository.getErrorTransaction(jtaGlobalXid);
		// if (jtaTransaction != null) {
		// jtaTransaction.registerTransactionListener(transaction);
		// }
		// }
	}

	public synchronized void timingRecover() {
		TransactionRepository transactionRepository = this.beanFactory.getTransactionRepository();
		List<Transaction> transactions = transactionRepository.getErrorTransactionList();
		for (int i = 0; i < transactions.size(); i++) {
			CompensableTransaction transaction = (CompensableTransaction) transactions.get(i);
			if (transaction.getTransactionContext().isCoordinator()) {
				// if (CompensableTccTransaction.class.isInstance(transaction)) {
				this.recoverCoordinatorTransaction((SampleCompensableImpl) transaction);
				// } else {
				// this.recoverTransaction((CompensableJtaTransaction) transaction);
				// }
			} else {
				// this.recoverParticipantTransaction((CompensableTccTransaction) transaction);
			}
		}
	}

	public void recoverCoordinatorTransaction(SampleCompensableImpl transaction) {
		CompensableLogger transactionLogger = this.beanFactory.getCompensableLogger();
		CompensableManager transactionManager = (CompensableManager) this.beanFactory.getCompensableManager();
		TransactionRepository transactionRepository = this.beanFactory.getTransactionRepository();

		TransactionContext transactionContext = transaction.getTransactionContext();
		boolean coordinator = transactionContext.isCoordinator();
		boolean coordinatorCancelFlags = false;

		TransactionXid xid = transactionContext.getXid();
		int compensableStatus = transaction.getCompensableStatus();
		int transactionStatus = transaction.getStatus();
		switch (transactionStatus) {
		case Status.STATUS_PREPARED:
			transaction.setCompensableStatus(SampleCompensableImpl.STATUS_TRIED);
			transaction.setTransactionStatus(Status.STATUS_COMMITTING);
			transaction.setCompensableStatus(SampleCompensableImpl.STATUS_CONFIRMING);
		case Status.STATUS_COMMITTING:
			if (compensableStatus != SampleCompensableImpl.STATUS_CONFIRMED) {
				try {
					transactionManager.processNativeConfirm(transaction);
				} catch (RuntimeException ex) {
					logger.warn(ex.getMessage(), ex);
					break;
				}
				transaction.setCompensableStatus(SampleCompensableImpl.STATUS_CONFIRMED);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());
			}

			try {
				transaction.remoteConfirm();
			} catch (Exception ex) {
				logger.debug(ex.getMessage(), ex);
				break;
			}

			transaction.setTransactionStatus(Status.STATUS_COMMITTED);
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			transactionRepository.removeTransaction(xid);
			transactionRepository.removeErrorTransaction(xid);

			break;
		case Status.STATUS_ACTIVE:
		case Status.STATUS_MARKED_ROLLBACK:
			transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
			transaction.setCompensableStatus(SampleCompensableImpl.STATUS_TRY_FAILURE);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());
			try {
				transaction.remoteCancel();
			} catch (Exception ex) {
				logger.debug(ex.getMessage(), ex);
				break;
			}

			transaction.setTransactionStatus(Status.STATUS_ROLLEDBACK);
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			transactionRepository.removeTransaction(xid);
			transactionRepository.removeErrorTransaction(xid);
			break;
		case Status.STATUS_PREPARING:
			transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
			if (coordinator
					&& (compensableStatus == SampleCompensableImpl.STATUS_TRIED || compensableStatus == SampleCompensableImpl.STATUS_TRY_MIXED)) {
				coordinatorCancelFlags = true;
			}
			transaction.setTransactionStatus(Status.STATUS_ROLLING_BACK);
			transactionLogger.updateTransaction(transaction.getTransactionArchive());
		case Status.STATUS_ROLLING_BACK:
			if (compensableStatus != SampleCompensableImpl.STATUS_CANCELLED
					&& compensableStatus != SampleCompensableImpl.STATUS_CANCEL_FAILURE) {
				try {
					if (coordinatorCancelFlags) {
						transactionManager.processNativeCancel(transaction, true);
					} else {
						transactionManager.processNativeCancel(transaction);
					}
				} catch (RuntimeException ex) {
					logger.warn(ex.getMessage(), ex);
					break;
				}
				transaction.setCompensableStatus(SampleCompensableImpl.STATUS_CANCELLED);
				transactionLogger.updateTransaction(transaction.getTransactionArchive());
			}

			try {
				transaction.remoteCancel();
			} catch (Exception ex) {
				logger.debug(ex.getMessage(), ex);
				break;
			}

			transaction.setTransactionStatus(Status.STATUS_ROLLEDBACK);
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			transactionRepository.removeTransaction(xid);
			transactionRepository.removeErrorTransaction(xid);

			break;
		case Status.STATUS_COMMITTED:
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			break;
		case Status.STATUS_ROLLEDBACK:
			transactionLogger.deleteTransaction(transaction.getTransactionArchive());
			break;
		}
	}

	public void setTransactionStatistic(TransactionStatistic transactionStatistic) {
		this.transactionStatistic = transactionStatistic;
	}

	public TransactionStatistic getTransactionStatistic() {
		return transactionStatistic;
	}

	public void setBeanFactory(CompensableBeanFactory tbf) {
		this.beanFactory = tbf;
	}

}