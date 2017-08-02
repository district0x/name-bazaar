pragma solidity ^0.4.14;

contract District0xEmails {

    mapping(address => string) public emails;

    function setEmail(string _encryptedEmail) {
        emails[msg.sender] = _encryptedEmail;
    }
}
