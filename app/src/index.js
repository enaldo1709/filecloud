import express from 'express';
import path from 'path';
import url from 'url';
import FileRequester from './requester/file-requester.js';
import multer from 'multer';

const app = express();
const port = 3000;

const SERVICE_HOST = process.env.SERVICE_HOST;
const SERVICE_PORT = process.env.SERVICE_PORT;

const requester = new FileRequester(SERVICE_HOST,SERVICE_PORT);

const __filename = url.fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const storage = multer.memoryStorage()
const uploader = multer({storage: storage})

app.use(express.static(__dirname + '/public'))

app.get('/list', async (req,res) => {
    await requester.list()
        .then(fl => {
            let list = []
            fl.forEach(f => {
                list.push(
                    {
                        name: f.name,
                        downloadURL: `/download?name=${f.name}`
                    }
                )
            });
            return list;
        })
        .then(data => res.status(200).send(JSON.stringify(data)))
}) 

app.post('/upload', uploader.single('file'), async (req,res) => {
    await requester.upload(req.file)
        .then(r => res.status(r.status).send(r.json()))
})

app.get('/download', async (req,res) => {
    await requester.download(req.query.name)
        .then(b => b.arrayBuffer())
        .then(b => {
            res.set("Content-Disposition", `attachment;filename="${req.query.name}"`)
            res.set("Content-Type", "application/octet-stream")
            res.status(200)
            res.write(new Uint8Array(b) )
            res.end()
        })
})

app.listen(port, (error) => {
    if(!error) 
        console.log("Server is Successfully Running, and App is listening on port "+ port);
    else 
        console.log("Error occurred, server can't start", error);
})
