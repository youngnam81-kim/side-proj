// src/App.jsx
import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import RowLayout from './components/layouts/RowLayout';
import BidBoardPage from './components/pages/BidBoardPage';
import ApiBoardPage from './components/pages/ApiBoardPage';
import AboutPage from './components/pages/AboutPage';
import QnABoardPage from './components/pages/QnABoardPage';

function App() {
  return (
    < BrowserRouter basename="/gov-bid-app" >
      <Routes>
        <Route path="/" element={<RowLayout />}>
          <Route index element={<ApiBoardPage />} />
          {/* <Route path="apiBoard" element={<ApiBoardPage />} /> */}
          <Route path="QnABoard" element={<QnABoardPage />} />
          <Route path="bidBoard" element={<BidBoardPage />} />
          <Route path="about" element={<AboutPage />} />
          <Route path="*" element={<div>404 Not Found</div>} />
        </Route>
      </Routes>
    </BrowserRouter >
  )
}

export default App
