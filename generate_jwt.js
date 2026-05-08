const crypto = require('crypto');

function createJwt(secret, email, role) {
    const header = { alg: 'HS256', typ: 'JWT' };
    const payload = {
        sub: email,
        role: role,
        iat: Math.floor(Date.now() / 1000),
        exp: Math.floor(Date.now() / 1000) + (24 * 60 * 60)
    };

    const base64UrlEncode = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64').replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
    
    const encodedHeader = base64UrlEncode(header);
    const encodedPayload = base64UrlEncode(payload);
    
    const signature = crypto.createHmac('sha256', secret)
                            .update(encodedHeader + '.' + encodedPayload)
                            .digest('base64').replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');

    return encodedHeader + '.' + encodedPayload + '.' + signature;
}

const secret = '7b5e4d3c2a1f09876543210fedcba9876543210abcdef0123456789abcdef012';
console.log(createJwt(secret, '1ds24is110@dsce.edu.in', 'STUDENT'));
