import multer from "multer";


class FileRequester {
    constructor(host, port) {
        this.url = (port && port != "") ? `http://${host}:${port}/filecloud` : `http://${host}/filecloud`;
    }

    async list() {
        return await fetch(`${this.url}/list`).then(r => r.json())
    } 

    async download(filename) {
        return await fetch(`${this.url}/download?filename=${filename}`)
            .then(res => res.blob());
    }

    async upload(file) {
        let formdata = new FormData()
        formdata.append('file', new Blob([new Uint8Array(file.buffer)]), file.originalname)

        
        return await fetch(`${this.url}/upload?filename=${file.originalname}`,{
            method: 'POST',
            body: formdata
        });
    }
}


export default FileRequester