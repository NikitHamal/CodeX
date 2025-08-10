package com.codex.apk;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TemplateManager {

    public String getBlankHtmlTemplate(String projectName) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + projectName + "</title>\n" +
                "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <h1>" + projectName + "</h1>\n" +
                "    <p>Welcome to your new project!</p>\n" +
                "    \n" +
                "    <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>";
    }

    public String getReactAppTemplate(String projectName) {
        return "import React from 'react';\n" +
                "import ReactDOM from 'react-dom';\n" +
                "import './App.css';\n" +
                "\n" +
                "function App() {\n" +
                "  return (\n" +
                "    <div className=\"App\">\n" +
                "      <header className=\"App-header\">\n" +
                "        <h1>" + projectName + "</h1>\n" +
                "        <p>Welcome to your React app!</p>\n" +
                "      </header>\n" +
                "    </div>\n" +
                "  );\n" +
                "}\n" +
                "\n" +
                "export default App;";
    }

    public String getNextJsAppTemplate(String projectName) {
        return "import Head from 'next/head';\n" +
                "import styles from '../styles/Home.module.css';\n" +
                "\n" +
                "export default function Home() {\n" +
                "  return (\n" +
                "    <div className={styles.container}>\n" +
                "      <Head>\n" +
                "        <title>" + projectName + "</title>\n" +
                "        <meta name=\"description\" content=\"Created with CodeX\" />\n" +
                "        <link rel=\"icon\" href=\"/favicon.ico\" />\n" +
                "      </Head>\n" +
                "\n" +
                "      <main className={styles.main}>\n" +
                "        <h1 className={styles.title}>Welcome to {projectName}</h1>\n" +
                "        <p className={styles.description}>Get started by editing pages/index.js</p>\n" +
                "      </main>\n" +
                "    </div>\n" +
                "  );\n" +
                "}";
    }

    public String getVueAppTemplate(String projectName) {
        return "<template>\n" +
                "  <div id=\"app\">\n" +
                "    <h1>{{ title }}</h1>\n" +
                "    <p>Welcome to your Vue.js app!</p>\n" +
                "  </div>\n" +
                "</template>\n" +
                "\n" +
                "<script>\n" +
                "export default {\n" +
                "  name: 'App',\n" +
                "  data() {\n" +
                "    return {\n" +
                "      title: '" + projectName + "'\n" +
                "    }\n" +
                "  }\n" +
                "}\n" +
                "</script>\n" +
                "\n" +
                "<style>\n" +
                "#app {\n" +
                "  font-family: Avenir, Helvetica, Arial, sans-serif;\n" +
                "  text-align: center;\n" +
                "  color: #2c3e50;\n" +
                "  margin-top: 60px;\n" +
                "}\n" +
                "</style>";
    }

    public String getAngularAppTemplate(String projectName) {
        return "import { Component } from '@angular/core';\n" +
                "\n" +
                "@Component({\n" +
                "  selector: 'app-root',\n" +
                "  template: `\n" +
                "    <div>\n" +
                "      <h1>{{ title }}</h1>\n" +
                "      <p>Welcome to your Angular app!</p>\n" +
                "    </div>\n" +
                "  `,\n" +
                "  styles: []\n" +
                "})\n" +
                "export class AppComponent {\n" +
                "  title = '" + projectName + "';\n" +
                "}";
    }

    public String getNodeBackendTemplate(String projectName) {
        return "const express = require('express');\n" +
                "const app = express();\n" +
                "const port = process.env.PORT || 3000;\n" +
                "\n" +
                "app.use(express.json());\n" +
                "app.use(express.static('public'));\n" +
                "\n" +
                "app.get('/', (req, res) => {\n" +
                "  res.json({ message: 'Welcome to " + projectName + " API' });\n" +
                "});\n" +
                "\n" +
                "app.listen(port, () => {\n" +
                "  console.log(`Server running on port ${port}`);\n" +
                "});";
    }

    public String getPythonBackendTemplate(String projectName) {
        return "from flask import Flask, jsonify\n" +
                "from flask_cors import CORS\n" +
                "\n" +
                "app = Flask(__name__)\n" +
                "CORS(app)\n" +
                "\n" +
                "@app.route('/')\n" +
                "def home():\n" +
                "    return jsonify({'message': 'Welcome to " + projectName + " API'})\n" +
                "\n" +
                "if __name__ == '__main__':\n" +
                "    app.run(debug=True, host='0.0.0.0', port=5000)";
    }

    public String getPhpBackendTemplate(String projectName) {
        return "<?php\n" +
                "header('Content-Type: application/json');\n" +
                "header('Access-Control-Allow-Origin: *');\n" +
                "\n" +
                "echo json_encode([\n" +
                "    'message' => 'Welcome to " + projectName + " API',\n" +
                "    'status' => 'success'\n" +
                "]);\n" +
                "?>";
    }

    public String getTailwindCssTemplate() {
        return "@tailwind base;\n" +
                "@tailwind components;\n" +
                "@tailwind utilities;\n" +
                "\n" +
                "/* Custom styles */\n" +
                ".custom-button {\n" +
                "    @apply bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded;\n" +
                "}";
    }

    public String getBootstrapTemplate() {
        return "/* Bootstrap customizations */\n" +
                ".custom-primary {\n" +
                "    background-color: #007bff;\n" +
                "    border-color: #007bff;\n" +
                "}\n" +
                "\n" +
                ".custom-primary:hover {\n" +
                "    background-color: #0056b3;\n" +
                "    border-color: #0056b3;\n" +
                "}";
    }

    public String getMaterialUiTemplate() {
        return "import { createTheme } from '@mui/material/styles';\n" +
                "\n" +
                "const theme = createTheme({\n" +
                "  palette: {\n" +
                "    primary: {\n" +
                "      main: '#1976d2',\n" +
                "    },\n" +
                "    secondary: {\n" +
                "      main: '#dc004e',\n" +
                "    },\n" +
                "  },\n" +
                "});\n" +
                "\n" +
                "export default theme;";
    }

    public String getPackageJsonTemplate(String projectName, String projectType) {
        switch (projectType) {
            case "react":
                return "{\n" +
                        "  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                        "  \"version\": \"0.1.0\",\n" +
                        "  \"private\": true,\n" +
                        "  \"dependencies\": {\n" +
                        "    \"react\": \"^18.2.0\",\n" +
                        "    \"react-dom\": \"^18.2.0\",\n" +
                        "    \"react-scripts\": \"5.0.1\"\n" +
                        "  },\n" +
                        "  \"scripts\": {\n" +
                        "    \"start\": \"react-scripts start\",\n" +
                        "    \"build\": \"react-scripts build\",\n" +
                        "    \"test\": \"react-scripts test\",\n" +
                        "    \"eject\": \"react-scripts eject\"\n" +
                        "  },\n" +
                        "  \"browserslist\": {\n" +
                        "    \"production\": [\n" +
                        "      \">0.2%\",\n" +
                        "      \"not dead\",\n" +
                        "      \"not op_mini all\"\n" +
                        "    ],\n" +
                        "    \"development\": [\n" +
                        "      \"last 1 chrome version\",\n" +
                        "      \"last 1 firefox version\",\n" +
                        "      \"last 1 safari version\"\n" +
                        "    ]\n" +
                        "  }\n" +
                        "}";
            case "nextjs":
                return "{\n" +
                        "  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                        "  \"version\": \"0.1.0\",\n" +
                        "  \"private\": true,\n" +
                        "  \"scripts\": {\n" +
                        "    \"dev\": \"next dev\",\n" +
                        "    \"build\": \"next build\",\n" +
                        "    \"start\": \"next start\",\n" +
                        "    \"lint\": \"next lint\"\n" +
                        "  },\n" +
                        "  \"dependencies\": {\n" +
                        "    \"next\": \"13.4.0\",\n" +
                        "    \"react\": \"18.2.0\",\n" +
                        "    \"react-dom\": \"18.2.0\"\n" +
                        "  },\n" +
                        "  \"devDependencies\": {\n" +
                        "    \"eslint\": \"8.0.0\",\n" +
                        "    \"eslint-config-next\": \"13.4.0\"\n" +
                        "  }\n" +
                        "}";
            case "vue":
                return "{\n" +
                        "  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                        "  \"version\": \"0.1.0\",\n" +
                        "  \"private\": true,\n" +
                        "  \"scripts\": {\n" +
                        "    \"serve\": \"vue-cli-service serve\",\n" +
                        "    \"build\": \"vue-cli-service build\",\n" +
                        "    \"lint\": \"vue-cli-service lint\"\n" +
                        "  },\n" +
                        "  \"dependencies\": {\n" +
                        "    \"vue\": \"^3.2.0\"\n" +
                        "  },\n" +
                        "  \"devDependencies\": {\n" +
                        "    \"@vue/cli-service\": \"^5.0.0\"\n" +
                        "  }\n" +
                        "}";
            case "angular":
                return "{\n" +
                        "  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                        "  \"version\": \"0.0.0\",\n" +
                        "  \"scripts\": {\n" +
                        "    \"ng\": \"ng\",\n" +
                        "    \"start\": \"ng serve\",\n" +
                        "    \"build\": \"ng build\",\n" +
                        "    \"watch\": \"ng build --watch --configuration development\"\n" +
                        "  },\n" +
                        "  \"private\": true,\n" +
                        "  \"dependencies\": {\n" +
                        "    \"@angular/animations\": \"^15.0.0\",\n" +
                        "    \"@angular/common\": \"^15.0.0\",\n" +
                        "    \"@angular/compiler\": \"^15.0.0\",\n" +
                        "    \"@angular/core\": \"^15.0.0\",\n" +
                        "    \"@angular/forms\": \"^15.0.0\",\n" +
                        "    \"@angular/platform-browser\": \"^15.0.0\",\n" +
                        "    \"@angular/platform-browser-dynamic\": \"^15.0.0\",\n" +
                        "    \"@angular/router\": \"^15.0.0\",\n" +
                        "    \"rxjs\": \"~7.5.0\",\n" +
                        "    \"tslib\": \"^2.3.0\",\n" +
                        "    \"zone.js\": \"~0.12.0\"\n" +
                        "  },\n" +
                        "  \"devDependencies\": {\n" +
                        "    \"@angular-devkit/build-angular\": \"^15.0.0\",\n" +
                        "    \"@angular/cli\": \"^15.0.0\",\n" +
                        "    \"@angular/compiler-cli\": \"^15.0.0\",\n" +
                        "    \"@types/jasmine\": \"~4.0.0\",\n" +
                        "    \"jasmine-core\": \"~4.0.0\",\n" +
                        "    \"karma\": \"~6.3.0\",\n" +
                        "    \"karma-chrome-launcher\": \"~3.1.0\",\n" +
                        "    \"karma-coverage\": \"~2.2.0\",\n" +
                        "    \"karma-jasmine\": \"~5.0.0\",\n" +
                        "    \"karma-jasmine-html-reporter\": \"~2.0.0\",\n" +
                        "    \"typescript\": \"~4.8.0\"\n" +
                        "  }\n" +
                        "}";
            case "node":
                return "{\n" +
                        "  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                        "  \"version\": \"1.0.0\",\n" +
                        "  \"description\": \"Node.js backend created with CodeX\",\n" +
                        "  \"main\": \"app.js\",\n" +
                        "  \"scripts\": {\n" +
                        "    \"start\": \"node app.js\",\n" +
                        "    \"dev\": \"nodemon app.js\"\n" +
                        "  },\n" +
                        "  \"dependencies\": {\n" +
                        "    \"express\": \"^4.18.0\",\n" +
                        "    \"cors\": \"^2.8.5\"\n" +
                        "  },\n" +
                        "  \"devDependencies\": {\n" +
                        "    \"nodemon\": \"^2.0.0\"\n" +
                        "  }\n" +
                        "}";
            default:
                return "{\n" +
                        "  \"name\": \"" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                        "  \"version\": \"1.0.0\",\n" +
                        "  \"description\": \"Project created with CodeX\"\n" +
                        "}";
        }
    }

    public String getRequirementsTxtTemplate() {
        return "Flask==2.3.0\n" +
                "Flask-CORS==4.0.0\n" +
                "python-dotenv==1.0.0";
    }

    public String getComposerJsonTemplate(String projectName) {
        return "{\n" +
                "  \"name\": \"codex/" + projectName.toLowerCase().replace(" ", "-") + "\",\n" +
                "  \"description\": \"PHP project created with CodeX\",\n" +
                "  \"type\": \"project\",\n" +
                "  \"require\": {\n" +
                "    \"php\": \">=7.4\"\n" +
                "  },\n" +
                "  \"autoload\": {\n" +
                "    \"psr-4\": {\n" +
                "      \"App\\\\\": \"src/\"\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getTailwindConfigTemplate() {
        return "module.exports = {\n" +
                "  content: [\n" +
                "    './src/**/*.{js,jsx,ts,tsx}',\n" +
                "    './public/index.html'\n" +
                "  ],\n" +
                "  theme: {\n" +
                "    extend: {},\n" +
                "  },\n" +
                "  plugins: [],\n" +
                "}";
    }

    public String getNextConfigTemplate() {
        return "/** @type {import('next').NextConfig} */\n" +
                "const nextConfig = {\n" +
                "  reactStrictMode: true,\n" +
                "  swcMinify: true,\n" +
                "}\n" +
                "\n" +
                "module.exports = nextConfig";
    }

    public String getVueConfigTemplate() {
        return "const { defineConfig } = require('@vue/cli-service')\n" +
                "module.exports = defineConfig({\n" +
                "  transpileDependencies: true,\n" +
                "  devServer: {\n" +
                "    port: 8080\n" +
                "  }\n" +
                "})";
    }

    public String getAngularConfigTemplate(String projectName) {
        return "{\n" +
                "  \"$schema\": \"./node_modules/@angular/cli/lib/config/schema.json\",\n" +
                "  \"version\": 1,\n" +
                "  \"newProjectRoot\": \"projects\",\n" +
                "  \"projects\": {\n" +
                "    \"" + projectName.toLowerCase().replace(" ", "-") + "\": {\n" +
                "      \"projectType\": \"application\",\n" +
                "      \"schematics\": {},\n" +
                "      \"root\": \"\",\n" +
                "      \"sourceRoot\": \"src\",\n" +
                "      \"prefix\": \"app\",\n" +
                "      \"architect\": {\n" +
                "        \"build\": {\n" +
                "          \"builder\": \"@angular-devkit/build-angular:browser\",\n" +
                "          \"options\": {\n" +
                "            \"outputPath\": \"dist\",\n" +
                "            \"index\": \"src/index.html\",\n" +
                "            \"main\": \"src/main.ts\",\n" +
                "            \"polyfills\": \"src/polyfills.ts\",\n" +
                "            \"tsConfig\": \"tsconfig.app.json\"\n" +
                "          }\n" +
                "        },\n" +
                "        \"serve\": {\n" +
                "          \"builder\": \"@angular-devkit/build-angular:dev-server\",\n" +
                "          \"options\": {\n" +
                "            \"browserTarget\": \"" + projectName.toLowerCase().replace(" ", "-") + ":build\"\n" +
                "          }\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }

    public String getBasicHtmlTemplate(String projectName) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + projectName + "</title>\n" +
                "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <h1>" + projectName + "</h1>\n" +
                "        <nav>\n" +
                "            <ul>\n" +
                "                <li><a href=\"#\">Home</a></li>\n" +
                "                <li><a href=\"#\">About</a></li>\n" +
                "                <li><a href=\"#\">Contact</a></li>\n" +
                "            </ul>\n" +
                "        </nav>\n" +
                "    </header>\n" +
                "    \n" +
                "    <main>\n" +
                "        <section>\n" +
                "            <h2>Welcome to " + projectName + "</h2>\n" +
                "            <p>This is a basic HTML template to get you started.</p>\n" +
                "            <button id=\"clickMe\">Click Me!</button>\n" +
                "        </section>\n" +
                "    </main>\n" +
                "    \n" +
                "    <footer>\n" +
                "        <p>&copy; " + new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()) + " " + projectName + ". All rights reserved.</p>\n" +
                "    </footer>\n" +
                "    \n" +
                "    <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>";
    }

    public String getBasicCssTemplate() {
        return "/* Basic CSS Template */\n" +
                "body {\n" +
                "    font-family: Arial, sans-serif;\n" +
                "    line-height: 1.6;\n" +
                "    margin: 0;\n" +
                "    padding: 0;\n" +
                "    color: #333;\n" +
                "}\n" +
                "\n" +
                "header {\n" +
                "    background-color: #2196F3;\n" +
                "    color: white;\n" +
                "    padding: 1rem;\n" +
                "    text-align: center;\n" +
                "}\n" +
                "\n" +
                "nav ul {\n" +
                "    list-style: none;\n" +
                "    display: flex;\n" +
                "    justify-content: center;\n" +
                "    margin-top: 1rem;\n" +
                "}\n" +
                "\n" +
                "nav ul li {\n" +
                "    margin: 0 1rem;\n" +
                "}\n" +
                "\n" +
                "nav ul li a {\n" +
                "    color: white;\n" +
                "    text-decoration: none;\n" +
                "}\n" +
                "\n" +
                "nav ul li a:hover {\n" +
                "    text-decoration: underline;\n" +
                "}\n" +
                "\n" +
                "main {\n" +
                "    max-width: 1200px;\n" +
                "    margin: 2rem auto;\n" +
                "    padding: 0 1rem;\n" +
                "}\n" +
                "\n" +
                "section {\n" +
                "    background-color: #f5f5f5;\n" +
                "    padding: 2rem;\n" +
                "    border-radius: 5px;\n" +
                "    box-shadow: 0 2px 5px rgba(0,0,0,0.1);\n" +
                "}\n" +
                "\n" +
                "h2 {\n" +
                "    color: #2196F3;\n" +
                "    margin-bottom: 1rem;\n" +
                "}\n" +
                "\n" +
                "p {\n" +
                "    margin-bottom: 1rem;\n" +
                "}\n" +
                "\n" +
                "button {\n" +
                "    background-color: #2196F3;\n" +
                "    color: white;\n" +
                "    border: none;\n" +
                "    padding: 0.5rem 1rem;\n" +
                "    border-radius: 4px;\n" +
                "    cursor: pointer;\n" +
                "    font-size: 1rem;\n" +
                "}\n" +
                "\n" +
                "button:hover {\n" +
                "    background-color: #0b7dda;\n" +
                "}\n" +
                "\n" +
                "footer {\n" +
                "    background-color: #333;\n" +
                "    color: white;\n" +
                "    text-align: center;\n" +
                "    padding: 1rem;\n" +
                "    margin-top: 2rem;\n" +
                "}";
    }

    public String getBasicJsTemplate() {
        return "// Basic JavaScript Template\n" +
                "document.addEventListener('DOMContentLoaded', function() {\n" +
                "    console.log('DOM fully loaded and parsed');\n" +
                "    \n" +
                "    // Get button element\n" +
                "    const button = document.getElementById('clickMe');\n" +
                "    \n" +
                "    // Add click event listener\n" +
                "    if(button) {\n" +
                "        button.addEventListener('click', function() {\n" +
                "            alert('Button clicked!');\n" +
                "            this.textContent = 'Clicked!';\n" +
                "            this.style.backgroundColor = '#4CAF50';\n" +
                "        });\n" +
                "    }\n" +
                "});";
    }

    public String getResponsiveHtmlTemplate(String projectName) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>" + projectName + "</title>\n" +
                "    <link rel=\"stylesheet\" href=\"style.css\">\n" +
                "    <link href=\"https://fonts.googleapis.com/icon?family=Material+Icons\" rel=\"stylesheet\">\n" +
                "</head>\n" +
                "<body>\n" +
                "    <header>\n" +
                "        <div class=\"container\">\n" +
                "            <h1>" + projectName + "</h1>\n" +
                "            <button class=\"menu-toggle\" id=\"menuToggle\">\n" +
                "                <span class=\"material-icons\">menu</span>\n" +
                "            </button>\n" +
                "            <nav id=\"mainNav\">\n" +
                "                <ul>\n" +
                "                    <li><a href=\"#\" class=\"active\">Home</a></li>\n" +
                "                    <li><a href=\"#\">About</a></li>\n" +
                "                    <li><a href=\"#\">Services</a></li>\n" +
                "                    <li><a href=\"#\">Portfolio</a></li>\n" +
                "                    <li><a href=\"#\">Contact</a></li>\n" +
                "                </ul>\n" +
                "            </nav>\n" +
                "        </div>\n" +
                "    </header>\n" +
                "    \n" +
                "    <main>\n" +
                "        <section class=\"hero\">\n" +
                "            <div class=\"container\">\n" +
                "                <h2>Welcome to " + projectName + "</h2>\n" +
                "                <p>A responsive website template created with CodeX.</p>\n" +
                "                <button class=\"cta-button\">Learn More</button>\n" +
                "            </div>\n" +
                "        </section>\n" +
                "        \n" +
                "        <section class=\"features\">\n" +
                "            <div class=\"container\">\n" +
                "                <h2>Our Features</h2>\n" +
                "                <div class=\"feature-grid\">\n" +
                "                    <div class=\"feature-card\">\n" +
                "                        <span class=\"material-icons\">devices</span>\n" +
                "                        <h3>Responsive Design</h3>\n" +
                "                        <p>Looks great on all devices, from mobile to desktop.</p>\n" +
                "                    </div>\n" +
                "                    <div class=\"feature-card\">\n" +
                "                        <span class=\"material-icons\">speed</span>\n" +
                "                        <h3>Fast Performance</h3>\n" +
                "                        <p>Optimized for speed and better user experience.</p>\n" +
                "                    </div>\n" +
                "                    <div class=\"feature-card\">\n" +
                "                        <span class=\"material-icons\">brush</span>\n" +
                "                        <h3>Modern Design</h3>\n" +
                "                        <p>Clean and modern interface with Material Design.</p>\n" +
                "                    </div>\n" +
                "                    <div class=\"feature-card\">\n" +
                "                        <span class=\"material-icons\">code</span>\n" +
                "                        <h3>Clean Code</h3>\n" +
                "                        <p>Well-structured and easy to customize.</p>\n" +
                "                    </div>\n" +
                "                </div>\n" +
                "            </div>\n" +
                "        </section>\n" +
                "    </main>\n" +
                "    \n" +
                "    <footer>\n" +
                "        <div class=\"container\">\n" +
                "            <p>&copy; " + new SimpleDateFormat("yyyy", Locale.getDefault()).format(new Date()) + " " + projectName + ". All rights reserved.</p>\n" +
                "            <div class=\"social-icons\">\n" +
                "                <a href=\"#\" class=\"social-icon\"><span class=\"material-icons\">facebook</span></a>\n" +
                "                <a href=\"#\" class=\"social-icon\"><span class=\"material-icons\">twitter</span></a>\n" +
                "                <a href=\"#\" class=\"social-icon\"><span class=\"material-icons\">instagram</span></a>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "    </footer>\n" +
                "    \n" +
                "    <script src=\"script.js\"></script>\n" +
                "</body>\n" +
                "</html>";
    }

    public String getResponsiveCssTemplate() {
        return "/* Responsive CSS Template */\n" +
                "* {\n" +
                "    box-sizing: border-box;\n" +
                "    margin: 0;\n" +
                "    padding: 0;\n" +
                "}\n" +
                "\n" +
                ":root {\n" +
                "    --primary-color: #2196F3;\n" +
                "    --primary-dark: #0b7dda;\n" +
                "    --secondary-color: #4CAF50;\n" +
                "    --text-color: #333;\n" +
                "    --light-bg: #f5f5f5;\n" +
                "    --dark-bg: #333;\n" +
                "}\n" +
                "\n" +
                "body {\n" +
                "    font-family: 'Roboto', 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
                "    line-height: 1.6;\n" +
                "    color: var(--text-color);\n" +
                "}\n" +
                "\n" +
                ".container {\n" +
                "    width: 100%;\n" +
                "    max-width: 1200px;\n" +
                "    margin: 0 auto;\n" +
                "    padding: 0 1rem;\n" +
                "}\n" +
                "\n" +
                "/* Header Styles */\n" +
                "header {\n" +
                "    background-color: var(--primary-color);\n" +
                "    color: white;\n" +
                "    padding: 1rem 0;\n" +
                "    position: sticky;\n" +
                "    top: 0;\n" +
                "    z-index: 100;\n" +
                "    box-shadow: 0 2px 5px rgba(0,0,0,0.1);\n" +
                "}\n" +
                "\n" +
                "header .container {\n" +
                "    display: flex;\n" +
                "    justify-content: space-between;\n" +
                "    align-items: center;\n" +
                "    flex-wrap: wrap;\n" +
                "}\n" +
                "\n" +
                "header h1 {\n" +
                "    font-size: 1.5rem;\n" +
                "    margin: 0;\n" +
                "}\n" +
                "\n" +
                ".menu-toggle {\n" +
                "    display: none;\n" +
                "    background: none;\n" +
                "    border: none;\n" +
                "    color: white;\n" +
                "    font-size: 1.5rem;\n" +
                "    cursor: pointer;\n" +
                "}\n" +
                "\n" +
                "nav ul {\n" +
                "    display: flex;\n" +
                "    list-style: none;\n" +
                "}\n" +
                "\n" +
                "nav ul li {\n" +
                "    margin-left: 1.5rem;\n" +
                "}\n" +
                "\n" +
                "nav ul li a {\n" +
                "    color: white;\n" +
                "    text-decoration: none;\n" +
                "    font-weight: 500;\n" +
                "    padding: 0.5rem 0;\n" +
                "    position: relative;\n" +
                "}\n" +
                "\n" +
                "nav ul li a:after {\n" +
                "    content: '';\n" +
                "    position: absolute;\n" +
                "    width: 0;\n" +
                "    height: 2px;\n" +
                "    bottom: 0;\n" +
                "    left: 0;\n" +
                "    background-color: white;\n" +
                "    transition: width 0.3s;\n" +
                "}\n" +
                "\n" +
                "nav ul li a:hover:after,\n" +
                "nav ul li a.active:after {\n" +
                "    width: 100%;\n" +
                "}\n" +
                "\n" +
                "/* Media Queries for Responsive Design */\n" +
                "@media screen and (max-width: 768px) {\n" +
                "    .menu-toggle {\n" +
                "        display: block;\n" +
                "    }\n" +
                "    \n" +
                "    nav {\n" +
                "        width: 100%;\n" +
                "        max-height: 0;\n" +
                "        overflow: hidden;\n" +
                "        transition: max-height 0.3s ease-out;\n" +
                "    }\n" +
                "    \n" +
                "    nav.active {\n" +
                "        max-height: 500px;\n" +
                "    }\n" +
                "    \n" +
                "    nav ul {\n" +
                "        flex-direction: column;\n" +
                "        padding: 1rem 0;\n" +
                "    }\n" +
                "    \n" +
                "    nav ul li {\n" +
                "        margin: 0.5rem 0;\n" +
                "    }\n" +
                "    \n" +
                "    .feature-grid {\n" +
                "        grid-template-columns: 1fr;\n" +
                "    }\n" +
                "}";
    }

    public String getResponsiveJsTemplate() {
        return "// Responsive JavaScript Template\n" +
                "document.addEventListener('DOMContentLoaded', function() {\n" +
                "    // Mobile menu toggle\n" +
                "    const menuToggle = document.getElementById('menuToggle');\n" +
                "    const mainNav = document.getElementById('mainNav');\n" +
                "    \n" +
                "    if (menuToggle && mainNav) {\n" +
                "        menuToggle.addEventListener('click', function() {\n" +
                "            mainNav.classList.toggle('active');\n" +
                "        });\n" +
                "    }\n" +
                "    \n" +
                "    // CTA button animation\n" +
                "    const ctaButton = document.querySelector('.cta-button');\n" +
                "    if (ctaButton) {\n" +
                "        ctaButton.addEventListener('click', function() {\n" +
                "            this.classList.add('clicked');\n" +
                "            setTimeout(() => {\n" +
                "                this.classList.remove('clicked');\n" +
                "                alert('Thanks for your interest! This is where you would add your call-to-action logic.');\n" +
                "            }, 300);\n" +
                "        });\n" +
                "    }\n" +
                "    \n" +
                "    // Feature card hover effects\n" +
                "    const featureCards = document.querySelectorAll('.feature-card');\n" +
                "    featureCards.forEach(card => {\n" +
                "        card.addEventListener('mouseenter', function() {\n" +
                "            this.style.transform = 'translateY(-15px)';\n" +
                "            this.style.boxShadow = '0 8px 16px rgba(0,0,0,0.2)';\n" +
                "        });\n" +
                "        \n" +
                "        card.addEventListener('mouseleave', function() {\n" +
                "            this.style.transform = 'translateY(-10px)';\n" +
                "            this.style.boxShadow = '0 4px 6px rgba(0,0,0,0.1)';\n" +
                "        });\n" +
                "    });\n" +
                "});";
    }
}