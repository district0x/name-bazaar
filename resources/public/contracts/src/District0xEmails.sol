pragma solidity ^0.5.0;

/**
 * @title District0xEmails
 * @dev Contract meant to be shared among all districts. Users can associate encypted email address
 * with their ethereum address. The purpose is to send email notifications to users.
 * It's encrypted by district0x public key and can be decrypted only by district0x servers
 */

contract District0xEmails {

    mapping(address => string) public emails;

    function setEmail(string calldata _encryptedEmail) external {
        emails[msg.sender] = _encryptedEmail;
    }

    function getEmail(address _address) external view returns(string memory) {
        return emails[_address];
    }
}
