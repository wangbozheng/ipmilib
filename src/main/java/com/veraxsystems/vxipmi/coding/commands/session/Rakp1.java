/*
 * Rakp1.java 
 * Created on 2011-07-28
 *
 * Copyright (c) Verax Systems 2011.
 * All rights reserved.
 *
 * This software is furnished under a license. Use, duplication,
 * disclosure and all other uses are restricted to the rights
 * specified in the written license agreement.
 */
package com.veraxsystems.vxipmi.coding.commands.session;

import com.veraxsystems.vxipmi.coding.commands.IpmiCommandCoder;
import com.veraxsystems.vxipmi.coding.commands.IpmiVersion;
import com.veraxsystems.vxipmi.coding.commands.PrivilegeLevel;
import com.veraxsystems.vxipmi.coding.commands.ResponseData;
import com.veraxsystems.vxipmi.coding.payload.CompletionCode;
import com.veraxsystems.vxipmi.coding.payload.IpmiPayload;
import com.veraxsystems.vxipmi.coding.payload.PlainMessage;
import com.veraxsystems.vxipmi.coding.payload.lan.IPMIException;
import com.veraxsystems.vxipmi.coding.payload.lan.NetworkFunction;
import com.veraxsystems.vxipmi.coding.protocol.AuthenticationType;
import com.veraxsystems.vxipmi.coding.protocol.IpmiMessage;
import com.veraxsystems.vxipmi.coding.protocol.Ipmiv20Message;
import com.veraxsystems.vxipmi.coding.protocol.PayloadType;
import com.veraxsystems.vxipmi.coding.security.CipherSuite;
import com.veraxsystems.vxipmi.coding.security.ConfidentialityNone;
import com.veraxsystems.vxipmi.common.Randomizer;
import com.veraxsystems.vxipmi.common.TypeConverter;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * <p>
 * A wrapper for RMCP+ RAKP1 message and it's response - RAKP2 message. The same
 * instance of this class that was used to prepare RAKP Message 1 should be also
 * used to decode matching RAKP Message 2 since the generated random number is
 * used in encryption process.
 * </p>
 * <p>
 * Capable of calculating SIK (Session Integrity Key) when RAKP Message 2 is
 * given ({@link #calculateSik(Rakp1ResponseData)}).
 * </p>
 */
public class Rakp1 extends IpmiCommandCoder {

	/**
	 * The Managed System's Session ID for this session. Must be as returned by
	 * the Managed System in the Open Session Response message.
	 */
	private int managedSystemSessionId;

	/**
	 * The random number generated by console.
	 */
	private byte[] consoleRandomNumber;

	private PrivilegeLevel requestedMaximumPrivilegeLevel;

	/**
	 * ASCII character Name that the user at the Remote Console wishes to assume
	 * for this session. It's length cannot exceed 16.
	 */
	private String username;

	/**
	 * Password matching username.
	 */
	private String password;

	/**
	 * Kg key associated with the target BMC. Should be null if Get Channel
	 * Authentication Capabilities Response indicated that Kg is disabled which
	 * means that 'one-key' logins are being used (
	 * {@link GetChannelAuthenticationCapabilitiesResponseData#isKgEnabled()} ==
	 * false)
	 */
	private byte[] bmcKey;

	public void setManagedSystemSessionId(int managedSystemSessionId) {
		this.managedSystemSessionId = managedSystemSessionId;
	}

	public int getManagedSystemSessionId() {
		return managedSystemSessionId;
	}

	public void setRequestedMaximumPrivilegeLevel(
			PrivilegeLevel requestedMaximumPrivilegeLevel) {
		this.requestedMaximumPrivilegeLevel = requestedMaximumPrivilegeLevel;
	}

	public PrivilegeLevel getRequestedMaximumPrivilegeLevel() {
		return requestedMaximumPrivilegeLevel;
	}

	public void setUsername(String username) {
		if (username.length() > 16) {
			throw new IllegalArgumentException(
					"Username is too long. It's length cannot exceed 16");
		}
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	private void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	private void setConsoleRandomNumber(byte[] randomNumber) {
		this.consoleRandomNumber = randomNumber;
	}

	public byte[] getConsoleRandomNumber() {
		return consoleRandomNumber;
	}

	private void setBmcKey(byte[] bmcKey) {
		this.bmcKey = bmcKey;
	}

	public byte[] getBmcKey() {
		return bmcKey;
	}

	/**
	 * Initiates class for encoding and decoding. Sets IPMI version to
	 * {@link IpmiVersion#V20} since RAKP1 is a RMCP+ command. Sets
	 * Authentication Type to RMCP+.
	 * 
	 * @param managedSystemSessionId
	 *            - The Managed System's Session ID for this session. Must be as
	 *            returned by the Managed System in the Open Session Response
	 *            message.
	 * @param privilegeLevel
	 *            - Requested Maximum {@link PrivilegeLevel}
	 * @param username
	 *            - ASCII character Name that the user at the Remote Console
	 *            wishes to assume for this session. It's length cannot exceed
	 *            16.
	 * @param password
	 *            - password matching username
	 * @param bmcKey
	 *            - BMC specific key. Should be null if Get Channel
	 *            Authentication Capabilities Response indicated that Kg is
	 *            disabled which means that 'one-key' logins are being used (
	 *            {@link GetChannelAuthenticationCapabilitiesResponseData#isKgEnabled()}
	 *            == false)
	 * @param cipherSuite
	 *            - {@link CipherSuite} containing authentication,
	 *            confidentiality and integrity algorithms for this session.
	 */
	public Rakp1(int managedSystemSessionId, PrivilegeLevel privilegeLevel,
			String username, String password, byte[] bmcKey,
			CipherSuite cipherSuite) {
		super(IpmiVersion.V20, cipherSuite, AuthenticationType.RMCPPlus);
		setManagedSystemSessionId(managedSystemSessionId);
		setRequestedMaximumPrivilegeLevel(privilegeLevel);
		setUsername(username);
		setPassword(password);
		this.setBmcKey(bmcKey);

		// prepare random number
		byte[] random = new byte[16];

		for (int i = 0; i < 4; ++i) {
			byte[] rand = TypeConverter.intToLittleEndianByteArray(Randomizer
					.getInt());

			System.arraycopy(rand, 0, random, 4 * i, 4);
		}

		setConsoleRandomNumber(random);
	}

	@Override
	public IpmiMessage encodeCommand(int sequenceNumber, int sessionId) {
		if (sessionId != 0) {
			throw new IllegalArgumentException("Session ID must be 0");
		}
		Ipmiv20Message message = new Ipmiv20Message(new ConfidentialityNone());

		message.setPayloadType(PayloadType.Rakp1);
		message.setSessionID(0);
		message.setSessionSequenceNumber(0);
		message.setAuthenticationType(getAuthenticationType());
		message.setPayloadAuthenticated(false);
		message.setPayloadEncrypted(false);

		message.setPayload(preparePayload(sequenceNumber));

		return message;
	}

	@Override
	protected IpmiPayload preparePayload(int sequenceNumber) {
		byte[] payload = null;

		if (getUsername() == null) {
			setUsername("");
		}

		payload = new byte[28 + getUsername().length()];

		// message tag
		payload[0] = TypeConverter.intToByte(sequenceNumber % 256);

		payload[1] = 0; // reserved
		payload[2] = 0; // reserved
		payload[3] = 0; // reserved

		byte[] sId = TypeConverter
				.intToLittleEndianByteArray(getManagedSystemSessionId());

		System.arraycopy(sId, 0, payload, 4, 4); // managed system session ID

		System.arraycopy(consoleRandomNumber, 0, payload, 8, 16); // generated
																	// random
		// number

		// requested privilege level; set name-only lookup
		payload[24] = TypeConverter
				.intToByte(encodePrivilegeLevel(requestedMaximumPrivilegeLevel) | 0x10);

		payload[25] = 0; // reserved
		payload[26] = 0; // reserved

		payload[27] = TypeConverter.intToByte(getUsername().length()); // username
																		// length

		if (getUsername().length() > 0) {
			System.arraycopy(getUsername().getBytes(), 0, payload, 28,
					getUsername().length()); // username
		}

		return new PlainMessage(payload);
	}

	@Override
	@Deprecated
	public byte getCommandCode() {
		return 0;
	}

	@Override
	@Deprecated
	public NetworkFunction getNetworkFunction() {
		return null;
	}

	/**
	 * @throws IllegalArgumentException
	 *             when message is not a response for class-specific command,
	 *             response has invalid length or authentication check fails.
	 * @throws NoSuchAlgorithmException
	 *             - when authentication, confidentiality or integrity algorithm
	 *             fails.
	 * @throws InvalidKeyException
	 *             - when creating of the algorithm key fails
	 */
	@Override
	public ResponseData getResponseData(IpmiMessage message)
			throws IllegalArgumentException, IPMIException,
			NoSuchAlgorithmException, InvalidKeyException {
		if (!isCommandResponse(message)) {
			throw new IllegalArgumentException("This is not RAKP 2 message!");
		}

		byte[] payload = message.getPayload().getPayloadData();

		Rakp1ResponseData data = new Rakp1ResponseData();

		data.setMessageTag(payload[0]);

		data.setStatusCode(payload[1]);

		if (payload[1] != 0) {
			throw new IPMIException(CompletionCode.parseInt(TypeConverter
					.byteToInt(payload[1])));
		}

		if (payload.length < 40) {
			throw new IllegalArgumentException("Invalid payload length");
		}

		byte[] buffer = new byte[4];

		System.arraycopy(payload, 4, buffer, 0, 4);

		data.setRemoteConsoleSessionId(TypeConverter
				.littleEndianByteArrayToInt(buffer));

		byte[] managedSystemGuid = new byte[16];

		System.arraycopy(payload, 24, managedSystemGuid, 0, 16);

		data.setManagedSystemGuid(managedSystemGuid);

		byte[] managedSystemRandomNumber = new byte[16];

		System.arraycopy(payload, 8, managedSystemRandomNumber, 0, 16);

		data.setManagedSystemRandomNumber(managedSystemRandomNumber);

		byte[] key = null;

		int length = getCipherSuite().getAuthenticationAlgorithm()
				.getKeyLength();

		if (length > 0) {
			key = new byte[length];
			System.arraycopy(payload, 40, key, 0, length);
		}

		if (!getCipherSuite().getAuthenticationAlgorithm()
				.checkKeyExchangeAuthenticationCode(
						prepareKeyExchangeAuthenticationCodeBase(data), key,
						getPassword())) {
			throw new IllegalArgumentException("Authentication check failed");
		}

		return data;
	}

	/**
	 * @return byte array holding prepared base for calculating
	 *         KeyExchangeAuthenticationCode for RAKP Message 2
	 */
	private byte[] prepareKeyExchangeAuthenticationCodeBase(
			Rakp1ResponseData responseData) {
		int length = 58;
		if (getUsername() != null) {
			length += getUsername().length();
		}
		byte[] keac = new byte[length];

		byte[] rSID = TypeConverter.intToLittleEndianByteArray(responseData
				.getRemoteConsoleSessionId());

		System.arraycopy(rSID, 0, keac, 0, 4);

		byte[] mSID = TypeConverter
				.intToLittleEndianByteArray(getManagedSystemSessionId());

		System.arraycopy(mSID, 0, keac, 4, 4);

		System.arraycopy(getConsoleRandomNumber(), 0, keac, 8, 16);

		System.arraycopy(responseData.getManagedSystemRandomNumber(), 0, keac,
				24, 16);

		System.arraycopy(responseData.getManagedSystemGuid(), 0, keac, 40, 16);

		keac[56] = TypeConverter
				.intToByte(encodePrivilegeLevel(requestedMaximumPrivilegeLevel) | 0x10);

		if (getUsername() != null) {
			keac[57] = TypeConverter.intToByte(getUsername().length());
			if (getUsername().length() > 0) {
				System.arraycopy(getUsername().getBytes(), 0, keac, 58,
						getUsername().length());
			}
		} else {
			keac[57] = 0;
		}

		return keac;
	}

	/**
	 * Calculates SIK (Session Integrity Key) based on RAKP Messages 1 and 2
	 * 
	 * @param responseData
	 *            RAKP Message 2 data
	 * @return Session Integrity Key
	 * @throws NoSuchAlgorithmException
	 *             - when authentication, confidentiality or integrity algorithm
	 *             fails.
	 * @throws InvalidKeyException
	 *             - when creating of the algorithm key fails
	 */
	public byte[] calculateSik(Rakp1ResponseData responseData)
			throws InvalidKeyException, NoSuchAlgorithmException {
		byte[] key = null;
		if (getBmcKey() == null || getBmcKey().length <= 0) {
			key = getPassword().getBytes();
		} else {
			key = getBmcKey();
		}

		return getCipherSuite().getAuthenticationAlgorithm()
				.getKeyExchangeAuthenticationCode(prepareSikBase(responseData),
						new String(key));
	}

	/**
	 * @return byte array holding prepared base for calculating Session
	 *         Integrity Key
	 */
	private byte[] prepareSikBase(Rakp1ResponseData responseData) {
		int length = 34;
		if (getUsername() != null) {
			length += getUsername().length();
		}

		byte[] sikBase = new byte[length];

		System.arraycopy(getConsoleRandomNumber(), 0, sikBase, 0, 16);

		System.arraycopy(responseData.getManagedSystemRandomNumber(), 0,
				sikBase, 16, 16);

		sikBase[32] = TypeConverter
				.intToByte(encodePrivilegeLevel(requestedMaximumPrivilegeLevel) | 0x10);

		if (getUsername() != null) {
			sikBase[33] = TypeConverter.intToByte(getUsername().length());
			if (getUsername().length() > 0) {
				System.arraycopy(getUsername().getBytes(), 0, sikBase, 34,
						getUsername().length());
			}
		} else {
			sikBase[33] = 0;
		}

		return sikBase;
	}

    @Override
    public boolean isCommandResponse(IpmiMessage message) {
        return message instanceof Ipmiv20Message && ((Ipmiv20Message) message).getPayloadType() == PayloadType.Rakp2;
    }
}
